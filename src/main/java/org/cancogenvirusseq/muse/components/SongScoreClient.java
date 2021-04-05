package org.cancogenvirusseq.muse.components;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.cancogenvirusseq.muse.model.song_score.AnalysisFileResponse;
import org.cancogenvirusseq.muse.model.song_score.ScoreFileSpec;
import org.cancogenvirusseq.muse.model.song_score.SubmitResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class SongScoreClient {

  @Value("${songScoreClient.songRootUrl}")
  String songRootUrl;

  @Value("${songScoreClient.scoreRootUrl}")
  String scoreRootUrl;

  @Value("${songScoreClient.systemApiToken}")
  String systemApiToken;

  @PostConstruct
  public void init() {
    log.info("Initialized song score client.");
    log.info("songRootUrl - " + songRootUrl);
    log.info("scoreRootUrl - " + scoreRootUrl);
  }

  public Mono<SubmitResponse> submitPayload(String studyId, String payload) {
    return WebClient.create(songRootUrl + "/submit/" + studyId)
        .post()
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(payload))
        .header("Authorization", "Bearer " + systemApiToken)
        .retrieve()
        .bodyToMono(SubmitResponse.class)
        .log();
  }

  public Mono<AnalysisFileResponse> getFileSpecFromSong(String studyId, UUID analysisId) {
    return WebClient.create(
            songRootUrl + "/studies/" + studyId + "/analysis/" + analysisId.toString() + "/files")
        .get()
        .retrieve()
        .bodyToFlux(AnalysisFileResponse.class)
        // we expect only one file to be uploaded in each analysis
        .next()
        .log();
  }

  public Mono<ScoreFileSpec> initScoreUpload(
      AnalysisFileResponse analysisFileResponse, String md5Sum) {
    val url =
        scoreRootUrl
            + "/upload/"
            + analysisFileResponse.getObjectId()
            + "/uploads?"
            + "fileSize="
            + analysisFileResponse.getFileSize()
            + "&md5="
            + md5Sum
            + "&overwrite=true";

    return WebClient.create(url)
        .post()
        .header("Authorization", "Bearer " + systemApiToken)
        .retrieve()
        .bodyToMono(ScoreFileSpec.class)
        .log();
  }

  // Mono<String> is etag
  public Mono<String> upload(ScoreFileSpec scoreFileSpec, String fileContent, String md5) {
    // we expect only one file part
    val presignedUrl = decodeUrl(scoreFileSpec.getParts().get(0).getUrl());

    return WebClient.create(presignedUrl)
        .put()
        .contentType(MediaType.TEXT_PLAIN)
        .contentLength(fileContent.length())
        .body(BodyInserters.fromValue(fileContent))
        .retrieve()
        .toBodilessEntity()
        .map(res -> res.getHeaders().getETag().replace("\"", ""))
        .flatMap(eTag -> finalizeScoreUpload(scoreFileSpec, md5, eTag))
        .log();
  }

  // The finalize step in score requires finalizing each file part and then the whole upload
  // we only have one file part, so we finalize the part and upload one after the other
  private Mono<String> finalizeScoreUpload(ScoreFileSpec scoreFileSpec, String md5, String etag) {
    val objectId = scoreFileSpec.getObjectId();
    val uploadId = scoreFileSpec.getUploadId();

    // finialize part publisher
    val finalizePartUrl =
        scoreRootUrl
            + "/upload/"
            + objectId
            + "/parts?"
            + "uploadId="
            + uploadId
            + "&etag="
            + etag
            + "&md5="
            + md5
            +
            // we expect only one file part
            "&partNumber=1";
    val finalizeUploadPart =
        WebClient.create(finalizePartUrl)
            .post()
            .header("Authorization", "Bearer " + systemApiToken)
            .retrieve()
            .toBodilessEntity();

    val finalizeUploadUrl = scoreRootUrl + "/upload/" + objectId + "?" + "uploadId=" + uploadId;
    val finalizeUpload =
        WebClient.create(finalizeUploadUrl)
            .post()
            .header("Authorization", "Bearer " + systemApiToken)
            .retrieve()
            .toBodilessEntity();

    return finalizeUploadPart.then(finalizeUpload).map(Objects::toString).log();
  }

  public Mono<String> publishAnalysis(String studyId, UUID analysisId) {
    return publishAnalysis(studyId, analysisId.toString());
  }

  public Mono<String> publishAnalysis(String studyId, String analysisId) {
    val url =
        songRootUrl
            + "/studies/"
            + studyId
            + "/analysis/publish/"
            + analysisId
            + "?ignoreUndefinedMd5=false";
    return WebClient.create(url)
        .put()
        .header("Authorization", "Bearer " + systemApiToken)
        .retrieve()
        .toBodilessEntity()
        .map(Objects::toString)
        .log();
  }

  public Mono<String> downloadObject(String objectId) {
    return getFileLink(objectId).flatMap(this::downloadFromS3);
  }

  public Mono<String> getFileLink(String objectId) {
    val url = scoreRootUrl + "/download/" + objectId + "?offset=0&length=-1&external=true";
    return WebClient.create(url)
        .get()
        .header("Authorization", "Bearer " + systemApiToken)
        .retrieve()
        .bodyToMono(ScoreFileSpec.class)
        // we request length = -1 which returns one file part
        .map(spec -> spec.getParts().get(0).getUrl());
  }

  public Mono<String> downloadFromS3(String presignedUrl) {
    return WebClient.create(decodeUrl(presignedUrl))
        .get()
        .retrieve()
        .bodyToMono(String.class)
        .log();
  }

  public static String decodeUrl(String str) {
    return URLDecoder.decode(str, StandardCharsets.UTF_8);
  }

  //  public static ClientDTO<Void> createPipeDto(String analysisId, String studyId, String md5) {
  //    return ClientDTO.<Void>builder()
  //        .analysisId(analysisId)
  //        .studyId(studyId)
  //        .fileMd5sum(md5)
  //        .build();
  //  }

  //  public static <E> ClientDTO<E> updateData(ClientDTO<?> dto, E data) {
  //    return ClientDTO.<E>builder()
  //        .analysisId(dto.getAnalysisId())
  //        .studyId(dto.getStudyId())
  //        .fileMd5sum(dto.getFileMd5sum())
  //        .data(data)
  //        .build();
  //  }

  //  @Builder
  //  @lombok.Value
  //  static class ClientDTO<T> {
  //    @NonNull String analysisId;
  //    @NonNull String studyId;
  //    @NonNull String fileMd5sum;
  //    T data;
  //  }
}
