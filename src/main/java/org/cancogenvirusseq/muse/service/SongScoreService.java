package org.cancogenvirusseq.muse.service;

import static org.cancogenvirusseq.muse.utils.AnalysisPayloadUtils.getFirstSubmitterSampleId;
import static org.cancogenvirusseq.muse.utils.AnalysisPayloadUtils.getStudyId;

import java.util.UUID;
import javax.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.cancogenvirusseq.muse.components.SongScoreClient;
import org.cancogenvirusseq.muse.model.SubmissionEvent;
import org.cancogenvirusseq.muse.model.SubmissionFile;
import org.cancogenvirusseq.muse.model.song_score.SongScoreServerException;
import org.cancogenvirusseq.muse.repository.UploadRepository;
import org.cancogenvirusseq.muse.repository.model.Upload;
import org.cancogenvirusseq.muse.repository.model.UploadStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;

@Slf4j
@Service
@RequiredArgsConstructor
public class SongScoreService {
  @Value("${upload.backpressure.highTide}")
  private Integer highTide;

  @Value("${upload.backpressure.lowTide}")
  private Integer lowTide;

  final SongScoreClient songScoreClient;
  final UploadRepository repo;

  private final Sinks.Many<SubmissionEvent> sink = Sinks.many().unicast().onBackpressureBuffer();

  @Getter private Disposable submitUploadDisposable;

  @PostConstruct
  public void init() {
    submitUploadDisposable = createSubmitUploadDisposable();
  }

  @Bean
  public Sinks.Many<SubmissionEvent> songScoreSubmitUploadSink() {
    return sink;
  }

  private Disposable createSubmitUploadDisposable() {
    return sink.asFlux()
        .flatMap(this::toStreamOfPayloadUploadAndSubFile)
        .flatMap(this::queueUpload)
        .flatMap(this::submitAndUploadToSongScore, 5, 1)
        .subscribe();
  }

  public Flux<Tuple3<String, Upload, SubmissionFile>> toStreamOfPayloadUploadAndSubFile(
      SubmissionEvent submissionEvent) {
    val submissionRequests = submissionEvent.getSubmissionRequests();

    return Flux.fromStream(
        submissionRequests.stream()
            .map(
                r -> {
                  val upload =
                      Upload.builder()
                          .studyId(getStudyId(r.getRecord()))
                          .submitterSampleId(getFirstSubmitterSampleId(r.getRecord()))
                          .submissionId(submissionEvent.getSubmissionId())
                          .userId(submissionEvent.getUserId())
                          .status(UploadStatus.QUEUED)
                          .originalFilePair(r.getOriginalFileNames())
                          .build();

                  log.debug(r.getRecord().toPrettyString());

                  return Tuples.of(r.getRecord().toString(), upload, r.getSubmissionFile());
                }));
  }

  public Mono<Tuple3<String, Upload, SubmissionFile>> queueUpload(Tuple3<String, Upload, SubmissionFile> tuple3) {
    return repo
               .save(tuple3.getT2())
               .map(updatedUpload -> Tuples.of(tuple3.getT1(), updatedUpload, tuple3.getT3()));
  }

  public Mono<Upload> submitAndUploadToSongScore(Tuple3<String, Upload, SubmissionFile> tuples3) {
    val payload = tuples3.getT1();
    val upload = tuples3.getT2();
    val submissionFile = tuples3.getT3();

    return songScoreClient.submitPayload(upload.getStudyId(), payload)
        .flatMap(
            submitResponse -> {
              upload.setAnalysisId(UUID.fromString(submitResponse.getAnalysisId()));
              upload.setStatus(UploadStatus.PROCESSING);
              return repo.save(upload);
            })
        .flatMap(u -> songScoreClient.getAnalysisFileFromSong(u.getStudyId(), u.getAnalysisId()))
        .flatMap(
            analysisFileResponse ->
                songScoreClient.initScoreUpload(
                    analysisFileResponse, submissionFile.getFileMd5sum()))
        .flatMap(
            scoreFileSpec ->
                songScoreClient.uploadAndFinalize(
                    scoreFileSpec, submissionFile.getContent(), submissionFile.getFileMd5sum()))
        .flatMap(
            res -> songScoreClient.publishAnalysis(upload.getStudyId(), upload.getAnalysisId()))
        .flatMap(
            r -> {
              upload.setStatus(UploadStatus.COMPLETE);
              return repo.save(upload);
            })
        .log("SongScoreService::submitAndUploadToSongScore")
        .onErrorResume(
            t -> {
              log.error(t.getLocalizedMessage(), t);
              upload.setStatus(UploadStatus.ERROR);
              if (t instanceof SongScoreServerException) {
                upload.setError(t.toString());
              } else if (Exceptions.isRetryExhausted(t)
                  && t.getCause() instanceof SongScoreServerException) {
                upload.setError(t.getCause().toString());
              } else {
                upload.setError("Internal server error!");
              }
              return repo.save(upload);
            });
  }
}
