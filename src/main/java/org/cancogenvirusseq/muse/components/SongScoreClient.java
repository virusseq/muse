package org.cancogenvirusseq.muse.components;

import static java.lang.String.format;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.cancogenvirusseq.muse.model.song_score.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ClientCredentialsReactiveOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.InMemoryReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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

  public Mono<Analysis> getAnalysis(String studyId, UUID analysisId) {
    return songClient
        .get()
        .uri(format("/studies/%s/analysis/%s", studyId, analysisId.toString()))
        .exchangeToMono(ofMonoTypeOrHandleError(Analysis.class))
        .map(HttpEntity::getBody)
        .log();
  }

  public Mono<SubmitResponse> submitPayload(String studyId, String payload) {
    return songClient
        .post()
        .uri(format("/submit/%s", studyId))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(payload))
        .exchangeToMono(ofMonoTypeOrHandleError(SubmitResponse.class))
        .map(HttpEntity::getBody)
        .log();
  }

  public Mono<AnalysisFile> getFileSpecFromSong(String studyId, UUID analysisId) {
    return songClient
        .get()
        .uri(format("/studies/%s/analysis/%s/files", studyId, analysisId.toString()))
        // endpoint returns array but, we expect only one file to be uploaded in each analysis
        .exchangeToFlux(ofFluxTypeOrHandleError(AnalysisFile.class))
        .next()
        .log();
  }

  public Mono<ScoreFileSpec> initScoreUpload(AnalysisFile analysisFile, String md5Sum) {
    val uri =
        format(
            "/upload/%s/uploads?fileSize=%s&md5=%s&overwrite=true",
            analysisFile.getObjectId(), analysisFile.getFileSize(), md5Sum);

    return scoreClient
        .post()
        .uri(uri)
        .exchangeToMono(ofMonoTypeOrHandleError(ScoreFileSpec.class))
        .map(HttpEntity::getBody)
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
        .exchangeToMono(ofBodilessEntityAndHandleError())
        .map(res -> res.getHeaders().getETag().replace("\"", ""))
        .flatMap(eTag -> finalizeScoreUpload(scoreFileSpec, md5, eTag))
        .log();
  }

  private Mono<String> finalizeScoreUpload(ScoreFileSpec scoreFileSpec, String md5, String etag) {
    val objectId = scoreFileSpec.getObjectId();
    val uploadId = scoreFileSpec.getUploadId();

    val finalizePartUri =
        format(
            "/upload/%s/parts?uploadId=%s&etag=%s&md5=%s&partNumber=1",
            objectId, uploadId, etag, md5);
    val finalizeUploadPart =
        scoreClient.post().uri(finalizePartUri).exchangeToMono(ofBodilessEntityAndHandleError());

    val finalizeUploadUri = format("/upload/%s?uploadId=%s", objectId, uploadId);
    val finalizeUpload =
        scoreClient.post().uri(finalizeUploadUri).exchangeToMono(ofBodilessEntityAndHandleError());

    // The finalize step in score requires finalizing each file part and then the whole upload
    // we only have one file part, so we finalize the part and upload one after the other
    return finalizeUploadPart.then(finalizeUpload).map(Objects::toString).log();
  }

  public Mono<String> publishAnalysis(String studyId, UUID analysisId) {
    return songClient
        .put()
        .uri(
            format("/studies/%s/analysis/publish/%s?ignoreUndefinedMd5=false", studyId, analysisId))
        .exchangeToMono(ofBodilessEntityAndHandleError())
        .map(Objects::toString)
        .log();
  }

  public Mono<DataBuffer> downloadObject(String objectId) {
    return getFileLink(objectId).flatMap(this::downloadFromS3);
  }

  private Mono<String> getFileLink(String objectId) {
    return scoreClient
        .get()
        .uri(format("/download/%s?offset=0&length=-1&external=true", objectId))
        .exchangeToMono(ofMonoTypeOrHandleError(ScoreFileSpec.class))
        .map(HttpEntity::getBody)
        // we request length = -1 which returns one file part
        .map(spec -> spec.getParts().get(0).getUrl());
  }

  private Mono<DataBuffer> downloadFromS3(String presignedUrl) {
    return WebClient.create(decodeUrl(presignedUrl))
        .get()
        .exchangeToMono(ofMonoTypeOrHandleError(DataBuffer.class))
        .map(HttpEntity::getBody)
        .log();
  }

  private static String decodeUrl(String str) {
    return URLDecoder.decode(str, StandardCharsets.UTF_8);
  }

  private static Function<ClientResponse, Mono<ResponseEntity<Void>>>
      ofBodilessEntityAndHandleError() {
    return ofMonoTypeOrHandleError(Void.class);
  }

  private static <V> Function<ClientResponse, Flux<V>> ofFluxTypeOrHandleError(Class<V> classType) {
    return clientResponse -> {
      val status = clientResponse.statusCode();
      if (clientResponse.statusCode().isError()) {
        return clientResponse
            .bodyToMono(ServerErrorResponse.class)
            .flux()
            .flatMap(res -> Flux.error(new SongScoreClientException(status, res.getMessage())));
      }

      return clientResponse.bodyToFlux(classType);
    };
  }

  private static <V> Function<ClientResponse, Mono<ResponseEntity<V>>> ofMonoTypeOrHandleError(
      Class<V> classType) {
    return clientResponse -> {
      if (clientResponse.statusCode().isError()) {
        return clientResponse
            .bodyToMono(ServerErrorResponse.class)
            .flatMap(
                res ->
                    Mono.error(
                        new SongScoreClientException(
                            clientResponse.statusCode(), res.getMessage())));
      }
      return clientResponse.toEntity(classType);
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
