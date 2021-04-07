package org.cancogenvirusseq.muse.components;

import static java.lang.String.format;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.cancogenvirusseq.muse.model.song_score.AnalysisFileResponse;
import org.cancogenvirusseq.muse.model.song_score.ScoreFileSpec;
import org.cancogenvirusseq.muse.model.song_score.SubmitResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class SongScoreClient {
  final WebClient songClient;
  final WebClient scoreClient;

  private static final String RESOURCE_ID_HEADER = "X-Resource-ID";
  private static final String OUATH_RESOURCE_ID = "songScoreOauth";

  public SongScoreClient(
      @Value("${songScoreClient.songRootUrl}") String songRootUrl,
      @Value("${songScoreClient.scoreRootUrl}") String scoreRootUrl,
      @Value("${songScoreClient.clientId}") String clientId,
      @Value("${songScoreClient.clientSecret}") String clientSecret,
      @Value("${songScoreClient.tokenUrl}") String tokenUrl) {

    val oauthFilter = createOauthFilter(OUATH_RESOURCE_ID, tokenUrl, clientId, clientSecret);

    songClient =
        WebClient.builder()
            .baseUrl(songRootUrl)
            .filter(oauthFilter)
            .defaultHeader(RESOURCE_ID_HEADER, OUATH_RESOURCE_ID)
            .build();

    scoreClient =
        WebClient.builder()
            .baseUrl(scoreRootUrl)
            .filter(oauthFilter)
            .defaultHeader(RESOURCE_ID_HEADER, OUATH_RESOURCE_ID)
            .build();

    log.info("Initialized song score client.");
    log.info("songRootUrl - " + songRootUrl);
    log.info("scoreRootUrl - " + scoreRootUrl);
  }

  public Mono<SubmitResponse> submitPayload(String studyId, String payload) {
    val uri = format("/submit/%s", studyId);
    return songClient
        .post()
        .uri(uri)
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(payload))
        .retrieve()
        .bodyToMono(SubmitResponse.class)
        .onErrorMap(logAndMapWithMsg("Failed to submit payload"))
        .log();
  }

  public Mono<AnalysisFileResponse> getFileSpecFromSong(String studyId, UUID analysisId) {
    val url = format("/studies/%s/analysis/%s/files", studyId, analysisId.toString());
    return songClient
        .get()
        .uri(url)
        .retrieve()
        .bodyToFlux(AnalysisFileResponse.class)
        // we expect only one file to be uploaded in each analysis
        .next()
        .onErrorMap(logAndMapWithMsg("Failed to get FileSpec from SONG"))
        .log();
  }

  public Mono<ScoreFileSpec> initScoreUpload(
      AnalysisFileResponse analysisFileResponse, String md5Sum) {
    val url =
        format(
            "/upload/%s/uploads?fileSize=%s&md5=%s&overwrite=true",
            analysisFileResponse.getObjectId(), analysisFileResponse.getFileSize(), md5Sum);

    return scoreClient
        .post()
        .uri(url)
        .retrieve()
        .bodyToMono(ScoreFileSpec.class)
        .onErrorMap(logAndMapWithMsg("Failed to initialize upload"))
        .log();
  }

  public Mono<String> uploadAndFinalize(
      ScoreFileSpec scoreFileSpec, String fileContent, String md5) {
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
        .onErrorMap(logAndMapWithMsg("Failed to upload and finalize"))
        .log();
  }

  private Mono<String> finalizeScoreUpload(ScoreFileSpec scoreFileSpec, String md5, String etag) {
    val objectId = scoreFileSpec.getObjectId();
    val uploadId = scoreFileSpec.getUploadId();

    val finalizePartUrl =
        format(
            "/upload/%s/parts?uploadId=%s&etag=%s&md5=%s&partNumber=1",
            objectId, uploadId, etag, md5);
    val finalizeUploadPart = scoreClient.post().uri(finalizePartUrl).retrieve().toBodilessEntity();

    val finalizeUploadUrl = format("/upload/%s?uploadId=%s", objectId, uploadId);
    val finalizeUpload = scoreClient.post().uri(finalizeUploadUrl).retrieve().toBodilessEntity();

    // The finalize step in score requires finalizing each file part and then the whole upload
    // we only have one file part, so we finalize the part and upload one after the other
    return finalizeUploadPart.then(finalizeUpload).map(Objects::toString).log();
  }

  public Mono<String> publishAnalysis(String studyId, UUID analysisId) {
    val url =
        format("/studies/%s/analysis/publish/%s?ignoreUndefinedMd5=false", studyId, analysisId);
    return songClient
        .put()
        .uri(url)
        .retrieve()
        .toBodilessEntity()
        .map(Objects::toString)
        .onErrorMap(logAndMapWithMsg("Failed to publish analysis"))
        .log();
  }

  public Mono<DataBuffer> downloadObject(String objectId) {
    return getFileLink(objectId)
        .flatMap(this::downloadFromS3)
        .onErrorMap(logAndMapWithMsg("Failed to publish analysis"));
  }

  private Mono<String> getFileLink(String objectId) {
    val url = format("/download/%s?offset=0&length=-1&external=true", objectId);
    return scoreClient
        .get()
        .uri(url)
        .retrieve()
        .bodyToMono(ScoreFileSpec.class)
        // we request length = -1 which returns one file part
        .map(spec -> spec.getParts().get(0).getUrl());
  }

  private Mono<DataBuffer> downloadFromS3(String presignedUrl) {
    return WebClient.create(decodeUrl(presignedUrl))
        .get()
        .retrieve()
        .bodyToMono(DataBuffer.class)
        .log();
  }

  private static String decodeUrl(String str) {
    return URLDecoder.decode(str, StandardCharsets.UTF_8);
  }

  private static Function<Throwable, Throwable> logAndMapWithMsg(String msg) {
    return t -> {
      t.printStackTrace();
      log.error("SongScoreClient Error - {}", t.getMessage());
      return new Error(msg);
    };
  }

  private ExchangeFilterFunction createOauthFilter(
      String regId, String tokenUrl, String clientId, String clientSecret) {
    // create client registration with Id for lookup by filter when needed
    val registration =
        ClientRegistration.withRegistrationId(regId)
            .tokenUri(tokenUrl)
            .clientId(clientId)
            .clientSecret(clientSecret)
            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            .build();
    val repo = new InMemoryReactiveClientRegistrationRepository(registration);

    // create new client manager to isolate from server oauth2 manager
    // more info: https://github.com/spring-projects/spring-security/issues/7984
    val authorizedClientService = new InMemoryReactiveOAuth2AuthorizedClientService(repo);
    val authorizedClientManager =
        new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(
            repo, authorizedClientService);
    authorizedClientManager.setAuthorizedClientProvider(
        new ClientCredentialsReactiveOAuth2AuthorizedClientProvider());

    // create filter function
    val oauth = new ServerOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);
    oauth.setDefaultClientRegistrationId(regId);
    return oauth;
  }
}
