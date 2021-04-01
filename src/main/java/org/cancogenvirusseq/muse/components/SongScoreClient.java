package org.cancogenvirusseq.muse.components;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import javax.annotation.PostConstruct;
import lombok.Builder;
import lombok.NonNull;
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
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

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

  // main function executing the song and dance
  public Mono<String> submitAndUpload(
      String studyId, String payload, String fileContent, String fileMd5) {
    return submitPayload(studyId, payload, fileMd5)
        .flatMap(this::getFileSpecFromSong)
        .flatMap(this::initScoreUpload)
        .flatMap(dto -> uploadToS3(dto, fileContent))
        .flatMap(this::finalizeScoreUpload)
        .flatMap(this::publishAnalysis);
  }

  private Mono<ClientDTO<Void>> submitPayload(String studyId, String payload, String md5) {
    return WebClient.create(songRootUrl + "/submit/" + studyId)
        .post()
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(payload))
        .header("Authorization", "Bearer " + systemApiToken)
        .retrieve()
        .bodyToMono(SubmitResponse.class)
        .map(submitResponse -> createPipeDto(submitResponse.getAnalysisId(), studyId, md5))
        .log();
  }


  private Mono<ClientDTO<AnalysisFileResponse>> getFileSpecFromSong(ClientDTO<Void> dto) {
    val studyId = dto.getStudyId();
    val analysisId = dto.getAnalysisId();
    return WebClient.create(
            songRootUrl + "/studies/" + studyId + "/analysis/" + analysisId + "/files")
        .get()
        .retrieve()
        .bodyToFlux(AnalysisFileResponse.class)
        // we expect only one file to be uploaded in each analysis
        .next()
        .map(analysisFileResponse -> updateData(dto, analysisFileResponse))
        .log();
  }

  private Mono<ClientDTO<ScoreFileSpec>> initScoreUpload(ClientDTO<AnalysisFileResponse> dto) {
    val analysisFileResponse = dto.getData();
    val url =
        scoreRootUrl
            + "/upload/"
            + analysisFileResponse.getObjectId()
            + "/uploads?"
            + "fileSize="
            + analysisFileResponse.getFileSize()
            + "&md5="
            + dto.getFileMd5sum()
            + "&overwrite=true";

    return WebClient.create(url)
        .post()
        .header("Authorization", "Bearer " + systemApiToken)
        .retrieve()
        .bodyToMono(ScoreFileSpec.class)
        .map(scoreFileSpec -> updateData(dto, scoreFileSpec))
        .log();
  }

  private Mono<ClientDTO<Tuple2<String, ScoreFileSpec>>> uploadToS3(
      ClientDTO<ScoreFileSpec> dto, String fileContent) {
    val scoreFileSpec = dto.getData();

    // we expect only one file part
    val presignedUrl =  decodeUrl(scoreFileSpec.getParts().get(0).getUrl());

    return WebClient.create(presignedUrl)
        .put()
        .contentType(MediaType.TEXT_PLAIN)
        .contentLength(fileContent.length())
        .body(BodyInserters.fromValue(fileContent))
        .retrieve()
        .toBodilessEntity()
        .map(res -> res.getHeaders().getETag().replace("\"", ""))
        .map(etag -> Tuples.of(etag, scoreFileSpec))
        .map(tuple2 -> updateData(dto, tuple2))
        .log();
  }

  private Mono<ClientDTO<String>> finalizeScoreUpload(
      ClientDTO<Tuple2<String, ScoreFileSpec>> dto) {
    val tuple2 = dto.getData();
    val scoreFileSpec = tuple2.getT2();
    val objectId = scoreFileSpec.getObjectId();
    val uploadId = scoreFileSpec.getUploadId();
    val etagOrMd5 = tuple2.getT1();

    // The finalize step in score requires finalizing each file part and then the whole upload
    // we only have one file part, so we finalize the part and upload one after the other

    // finialize part publisher
    val finalizePartUrl =
        scoreRootUrl
            + "/upload/"
            + objectId
            + "/parts?"
            + "uploadId="
            + uploadId
            + "&etag="
            + etagOrMd5
            + "&md5="
            + dto.getFileMd5sum()
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

    return finalizeUploadPart
        .then(finalizeUpload)
        .map(Objects::toString)
        .map(str -> updateData(dto, str))
        .log();
  }

  private Mono<String> publishAnalysis(ClientDTO<String> dto) {
    val analysisId = dto.getAnalysisId();
    val studyId = dto.getStudyId();

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

  private Mono<String> getFileLink(String objectId) {
    val url = scoreRootUrl + "/download/" + objectId + "?offset=0&length=-1&external=true";
    return WebClient
                   .create(url)
                   .get()
                   .header("Authorization", "Bearer " + systemApiToken)
                   .retrieve()
                   .bodyToMono(ScoreFileSpec.class)

                   // we request length = -1 which returns one file part
                   .map(spec -> spec.getParts().get(0).getUrl());
  }

  private Mono<String> downloadFromS3(String presignedUrl) {
    return WebClient.create(decodeUrl(presignedUrl)).get().retrieve().bodyToMono(String.class).log();
  }

  private static String decodeUrl(String str) {
    return URLDecoder.decode(str, StandardCharsets.UTF_8);
  }
  
  private static ClientDTO<Void> createPipeDto(String analysisId, String studyId, String md5) {
    return ClientDTO.<Void>builder()
        .analysisId(analysisId)
        .studyId(studyId)
        .fileMd5sum(md5)
        .build();
  }

  private static <E> ClientDTO<E> updateData(ClientDTO<?> dto, E data) {
    return ClientDTO.<E>builder()
        .analysisId(dto.getAnalysisId())
        .studyId(dto.getStudyId())
        .fileMd5sum(dto.getFileMd5sum())
        .data(data)
        .build();
  }

  @Builder
  @lombok.Value
  static class ClientDTO<T> {
    @NonNull String analysisId;
    @NonNull String studyId;
    @NonNull String fileMd5sum;
    T data;
  }
}
