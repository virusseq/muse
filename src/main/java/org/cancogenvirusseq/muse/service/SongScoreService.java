package org.cancogenvirusseq.muse.service;

import static org.cancogenvirusseq.muse.utils.AnalysisPayloadUtils.getFirstSubmitterSampleId;
import static org.cancogenvirusseq.muse.utils.AnalysisPayloadUtils.getStudyId;

import java.time.Duration;
import java.util.List;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;

@Slf4j
@Service
@RequiredArgsConstructor
public class SongScoreService {
  @Value("${codeConfig.uploadPipelineDelaySec}")
  private Integer uploadPipelineDelaySec;

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
                          .originalFilePair(List.of(r.getRecordFilename(), r.getSampleFilename()))
                          .build();

                  log.debug(r.getRecord().toPrettyString());

                  return Tuples.of(r.getRecord().toString(), upload, r.getSubmissionFile());
                }));
  }

  public Mono<Upload> submitAndUploadToSongScore(Tuple3<String, Upload, SubmissionFile> tuples3) {
    val payload = tuples3.getT1();
    val upload = tuples3.getT2();
    val submissionFile = tuples3.getT3();

    // add delay between each step in pipeline to give SONG/score/postgres some breathing room
    // e.g. prevents potential 500s from SONG because it can't get a connection to its db
    return repo.save(upload)
        .delayElement(Duration.ofSeconds(uploadPipelineDelaySec))
        .flatMap(u -> songScoreClient.submitPayload(u.getStudyId(), payload))
        .delayElement(Duration.ofSeconds(uploadPipelineDelaySec))
        .flatMap(
            submitResponse -> {
              upload.setAnalysisId(UUID.fromString(submitResponse.getAnalysisId()));
              upload.setStatus(UploadStatus.PROCESSING);
              return repo.save(upload);
            })
        .delayElement(Duration.ofSeconds(uploadPipelineDelaySec))
        .flatMap(u -> songScoreClient.getAnalysisFileFromSong(u.getStudyId(), u.getAnalysisId()))
        .delayElement(Duration.ofSeconds(uploadPipelineDelaySec))
        .flatMap(
            analysisFileResponse ->
                songScoreClient.initScoreUpload(
                    analysisFileResponse, submissionFile.getFileMd5sum()))
        .delayElement(Duration.ofSeconds(3))
        .flatMap(
            scoreFileSpec ->
                songScoreClient.uploadAndFinalize(
                    scoreFileSpec, submissionFile.getContent(), submissionFile.getFileMd5sum()))
        .delayElement(Duration.ofSeconds(uploadPipelineDelaySec))
        .flatMap(
            res -> songScoreClient.publishAnalysis(upload.getStudyId(), upload.getAnalysisId()))
        .delayElement(Duration.ofSeconds(uploadPipelineDelaySec))
        .flatMap(
            r -> {
              upload.setStatus(UploadStatus.COMPLETE);
              return repo.save(upload);
            })
        .delayElement(Duration.ofSeconds(uploadPipelineDelaySec))
        // Using Schedulers.single per upload to ensure the repo connection is controlled.
        // Other wise default scheduler spawns many connections to db, and postgres will
        // complain about too many clients connected.
        .subscribeOn(Schedulers.single())
        .onErrorResume(
            t -> {
              log.error(t.getLocalizedMessage(), t);
              upload.setStatus(UploadStatus.ERROR);
              if (t instanceof SongScoreServerException) {
                upload.setError(t.toString());
              } else {
                upload.setError("Internal server error - unrelated to SongScore!");
              }
              return repo.save(upload);
            });
  }
}
