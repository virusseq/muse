package org.cancogenvirusseq.muse.service;

import static org.cancogenvirusseq.muse.utils.AnalysisPayloadUtils.getFirstSubmitterSampleId;
import static org.cancogenvirusseq.muse.utils.AnalysisPayloadUtils.getStudyId;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import javax.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.cancogenvirusseq.muse.components.SongScoreClient;
import org.cancogenvirusseq.muse.model.SubmissionEvent;
import org.cancogenvirusseq.muse.model.SubmissionFile;
import org.cancogenvirusseq.muse.model.song_score.SongScoreClientException;
import org.cancogenvirusseq.muse.repository.UploadRepository;
import org.cancogenvirusseq.muse.repository.model.Upload;
import org.cancogenvirusseq.muse.repository.model.UploadStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;

@Slf4j
@Service
@RequiredArgsConstructor
public class SongScoreService {
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
        .flatMap(this::submitAndUploadToSongScore)
        .subscribe();
  }

  public Flux<Tuple3<String, Upload, SubmissionFile>> toStreamOfPayloadUploadAndSubFile(
      SubmissionEvent submissionEvent) {
    val records = submissionEvent.getPayloadFileTuples();

    return Flux.fromStream(
        records
            .parallelStream()
            .map(
                r -> {
                  val payload = r.getT1();
                  val submissionFile = r.getT2();

                  val sampleId = getFirstSubmitterSampleId(payload);
                  val studyId = getStudyId(payload);

                  val upload =
                      Upload.builder()
                          .studyId(studyId)
                          .submitterSampleId(sampleId)
                          .submissionId(submissionEvent.getSubmissionId())
                          .userId(submissionEvent.getUserId())
                          .status(UploadStatus.QUEUED)
                          .originalFilePair(List.of(submissionFile.getFileName()))
                          .build();

                  log.debug(payload.toPrettyString());

                  return Tuples.of(payload.toString(), upload, submissionFile);
                }));
  }

  public Mono<Upload> submitAndUploadToSongScore(Tuple3<String, Upload, SubmissionFile> tuples3) {
    val payload = tuples3.getT1();
    val upload = tuples3.getT2();
    val submissionFile = tuples3.getT3();

    return repo.save(upload)
        .flatMap(u -> songScoreClient.submitPayload(u.getStudyId(), payload))
        .flatMap(
            submitResponse -> {
              upload.setAnalysisId(UUID.fromString(submitResponse.getAnalysisId()));
              upload.setStatus(UploadStatus.PROCESSING);
              return repo.save(upload);
            })
        .flatMap(u -> songScoreClient.getFileSpecFromSong(u.getStudyId(), u.getAnalysisId()))
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
        .onErrorResume(
            t -> {
              log.error(t.getLocalizedMessage(), t);
              upload.setStatus(UploadStatus.ERROR);
              if (t instanceof SongScoreClientException) {
                upload.setError(((SongScoreClientException) t).getSongScoreErrorMsg());
              } else {
                upload.setError(t.getLocalizedMessage());
              }
              return repo.save(upload);
            });
  }

  // TODO: consider handling webclient errors here?
  private static Function<Throwable, Throwable> logAndMapWithMsg(String msg) {
    return t -> {
      log.error("SongScoreClient Error - {}", t.getLocalizedMessage(), t);
      return new Error(msg);
    };
  }
}
