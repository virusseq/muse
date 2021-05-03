package org.cancogenvirusseq.muse.service;

import java.util.UUID;
import javax.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.cancogenvirusseq.muse.components.SongScoreClient;
import org.cancogenvirusseq.muse.config.db.PostgresProperties;
import org.cancogenvirusseq.muse.model.UploadEvent;
import org.cancogenvirusseq.muse.model.song_score.SongScoreServerException;
import org.cancogenvirusseq.muse.repository.model.Upload;
import org.cancogenvirusseq.muse.repository.model.UploadStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

@Slf4j
@Service
@RequiredArgsConstructor
public class SongScoreService {
  @Value("${submitUpload.maxInFlight}")
  private Integer maxInFlight;

  // Prefetch determines max in-flight elements from inner Publisher sequence
  // all Publishers in submitAndUploadToSongScore return Mono, so only one element
  private static final Integer SONG_SCORE_SUBMIT_UPLOAD_PREFETCH = 1;

  final UploadService uploadService;
  final SongScoreClient songScoreClient;
  final PostgresProperties props;

  private final Sinks.Many<UploadEvent> sink = Sinks.many().unicast().onBackpressureBuffer();

  @Getter private Disposable submitUploadDisposable;

  @PostConstruct
  public void init() {
    submitUploadDisposable = createSubmitUploadDisposable();
  }

  @Bean
  public Sinks.Many<UploadEvent> songScoreSubmitUploadSink() {
    return sink;
  }

  private Disposable createSubmitUploadDisposable() {
    return sink.asFlux()
        // Concurrency of this flatMap is controlled to not overwhelm SONG/score
        .flatMap(this::submitAndUploadToSongScore, maxInFlight, SONG_SCORE_SUBMIT_UPLOAD_PREFETCH)
        .subscribe();
  }

  public Mono<Upload> submitAndUploadToSongScore(UploadEvent uploadEvent) {
    // upload will be mutated and saved as it goes through the pipe
    val upload = uploadEvent.getUpload();

    return songScoreClient
        .submitPayload(uploadEvent.getStudyId(), uploadEvent.getPayload())
        .flatMap(
            submitResponse -> {
              upload.setAnalysisId(UUID.fromString(submitResponse.getAnalysisId()));
              upload.setStatus(UploadStatus.PROCESSING);
              return uploadService.updateUpload(upload);
            })
        .flatMap(
            updatedUpload ->
                songScoreClient.getAnalysisFileFromSong(
                    updatedUpload.getStudyId(), updatedUpload.getAnalysisId()))
        .flatMap(
            analysisFileResponse ->
                songScoreClient.initScoreUpload(
                    analysisFileResponse, uploadEvent.getSubmissionFile().getFileMd5sum()))
        .flatMap(
            scoreFileSpec ->
                songScoreClient.uploadAndFinalize(
                    scoreFileSpec,
                    uploadEvent.getSubmissionFile().getContent(),
                    uploadEvent.getSubmissionFile().getFileMd5sum()))
        .flatMap(
            res -> songScoreClient.publishAnalysis(upload.getStudyId(), upload.getAnalysisId()))
        .flatMap(
            r -> {
              upload.setStatus(UploadStatus.COMPLETE);
              return uploadService.updateUpload(upload);
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
              return uploadService.updateUpload(upload);
            });
  }
}
