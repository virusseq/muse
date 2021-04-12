package org.cancogenvirusseq.muse.components;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.cancogenvirusseq.muse.model.song_score.AnalysisFileResponse;
import org.cancogenvirusseq.muse.model.song_score.ScoreFileSpec;
import org.cancogenvirusseq.muse.model.song_score.SubmitResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ClientCredentialsReactiveOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.InMemoryReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

import static java.lang.String.format;

@Slf4j
@Component
public class SongScoreClient {
  final WebClient songClient;
  final WebClient scoreClient;

  private static final String RESOURCE_ID_HEADER = "X-Resource-ID";
  private static final String OUATH_RESOURCE_ID = "songScoreOauth";

  @Autowired
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

  public SongScoreClient(@NonNull WebClient songClient, @NonNull WebClient scoreClient) {
    this.songClient = songClient;
    this.scoreClient = scoreClient;

    log.info("Initialized song score client.");
  }

  public Mono<SubmitResponse> submitPayload(String studyId, String payload) {
    return songClient
        .post()
        .uri(format("/submit/%s", studyId))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(payload))
        .retrieve()
        .bodyToMono(SubmitResponse.class)
        .onErrorMap(logAndMapWithMsg("Failed to submit payload"))
        .log();
  }

  public Mono<AnalysisFileResponse> getFileSpecFromSong(String studyId, UUID analysisId) {
    return songClient
        .get()
        .uri(format("/studies/%s/analysis/%s/files", studyId, analysisId.toString()))
        .retrieve()
        .bodyToFlux(AnalysisFileResponse.class)
        // we expect only one file to be uploaded in each analysis
        .next()
        // TODO: handle song exceptions here for analysis not found, etc.
        .onErrorMap(logAndMapWithMsg("Failed to get FileSpec from SONG"))
        .log();
  }

  public Mono<ScoreFileSpec> initScoreUpload(
      AnalysisFileResponse analysisFileResponse, String md5Sum) {
    val uri =
        format(
            "/upload/%s/uploads?fileSize=%s&md5=%s&overwrite=true",
            analysisFileResponse.getObjectId(), analysisFileResponse.getFileSize(), md5Sum);

    return scoreClient
        .post()
        .uri(uri)
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

    val finalizePartUri =
        format(
            "/upload/%s/parts?uploadId=%s&etag=%s&md5=%s&partNumber=1",
            objectId, uploadId, etag, md5);
    val finalizeUploadPart = scoreClient.post().uri(finalizePartUri).retrieve().toBodilessEntity();

    val finalizeUploadUri = format("/upload/%s?uploadId=%s", objectId, uploadId);
    val finalizeUpload = scoreClient.post().uri(finalizeUploadUri).retrieve().toBodilessEntity();

    // The finalize step in score requires finalizing each file part and then the whole upload
    // we only have one file part, so we finalize the part and upload one after the other
    return finalizeUploadPart.then(finalizeUpload).map(Objects::toString).log();
  }

  public Mono<String> publishAnalysis(String studyId, UUID analysisId) {
    return songClient
        .put()
        .uri(
            format("/studies/%s/analysis/publish/%s?ignoreUndefinedMd5=false", studyId, analysisId))
        .retrieve()
        .toBodilessEntity()
        .map(Objects::toString)
        .onErrorMap(logAndMapWithMsg("Failed to publish analysis"))
        .log();
  }

  public Mono<DataBuffer> downloadObject(String objectId) {
    return getFileLink(objectId)
        .flatMap(this::downloadFromS3)
        // todo: either map to something directly or handle centrally in logAndMapWithMsg
        .onErrorMap(logAndMapWithMsg("Object download failed"));
  }

  private Mono<String> getFileLink(String objectId) {
    return scoreClient
        .get()
        .uri(format("/download/%s?offset=0&length=-1&external=true", objectId))
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

  // TODO: consider handling webclient errors here?
  private static Function<Throwable, Throwable> logAndMapWithMsg(String msg) {
    return t -> {
      log.error("SongScoreClient Error - {}", t.getLocalizedMessage(), t);
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
