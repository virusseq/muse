package org.cancogenvirusseq.muse.components.songscore;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.cancogenvirusseq.muse.components.songscore.model.AnalysisFileResponse;
import org.cancogenvirusseq.muse.components.songscore.model.ScoreFileSpec;
import org.cancogenvirusseq.muse.components.songscore.model.SubmitResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

@Slf4j
@Service
public class SubmissionToSongScore {

    String songRootUrl = "https://song.rdpc-qa.cancercollaboratory.org/";
    String scoreRootUrl = "https://score.rdpc-qa.cancercollaboratory.org/";

    String token = "convertmetooauth";

    // main function executing the song and dance
    public Mono<String> submitAndUpload(String studyId, String payload, String fileContent) {
        return submitPayload(studyId, payload)
                       .flatMap(submitResponse -> {
                           val analysisId = submitResponse.getAnalysisId();
                           val dto = createPipeDto(analysisId, studyId);
                           return getFileId(dto);
                       })
                       .flatMap(this::initScoreUpload)
                       .flatMap(dto -> uploadToS3(dto, fileContent))
                       .flatMap(this::finalizeScoreUpload)
                       .flatMap(this::publishAnalysis);
    }

    private Mono<SubmitResponse> submitPayload(String studyId, String payload) {
        return WebClient.create(songRootUrl + "/submit/" + studyId)
                       .post()
                       .contentType(MediaType.APPLICATION_JSON)
                       .body(BodyInserters.fromValue(payload))
                       .header("Authorization", "Bearer " + token)
                       .retrieve()
                       .bodyToMono(SubmitResponse.class)
                       .log();
    }

    private Mono<PipeDTO<AnalysisFileResponse>> getFileId(PipeDTO<Void> dto) {
        val studyId = dto.getStudyId();
        val analysisId = dto.getAnalysisId();
        return WebClient.create(songRootUrl + "/studies/" + studyId + "/analysis/" + analysisId + "/files")
                       .get()
                       .retrieve()
                       .bodyToFlux(AnalysisFileResponse.class)
                       // we expect only one file to be uploaded in each analysis
                       .next()
                       .map(analysisFileResponse -> updateData(dto, analysisFileResponse))
                       .log();
    }

    private Mono<PipeDTO<ScoreFileSpec>> initScoreUpload(PipeDTO<AnalysisFileResponse> dto) {
        val analysisFileResponse = dto.getData();
        val url = scoreRootUrl + "/upload/" + analysisFileResponse.getObjectId() + "/uploads?" +
                          "fileSize=" + analysisFileResponse.getFileSize() +
                          "&overwrite=true";

        return WebClient
                       .create(url)
                       .post()
                       .header("Authorization", "Bearer " + token)
                       .retrieve()
                       .bodyToMono(ScoreFileSpec.class)
                       .map(scoreFileSpec -> updateData(dto, scoreFileSpec))
                       .log();
    }

    private Mono<PipeDTO<Tuple2<String, ScoreFileSpec>>> uploadToS3(PipeDTO<ScoreFileSpec> dto, String fileContent) {
        val scoreFileSpec = dto.getData();

        // we expect only one file part
        // also the url is decoded because Webclient encodes the url and we end up with double encode
        val presignedUrl = URLDecoder.decode(scoreFileSpec.getParts().get(0).getUrl(), StandardCharsets.UTF_8);
        return WebClient
                       .create(presignedUrl)
                       .put()
                       .contentType(MediaType.TEXT_PLAIN)
                       .contentLength(fileContent.length())
                       .body(BodyInserters.fromValue(fileContent))
                       .retrieve().toBodilessEntity()
                       .map(res -> res.getHeaders().getETag().replace("\"", ""))
                       .map(etag -> Tuples.of(etag, scoreFileSpec))
                       .map(tuple2 -> updateData(dto, tuple2))
                       .log();
    }

    private Mono<PipeDTO<String>> finalizeScoreUpload(PipeDTO<Tuple2<String, ScoreFileSpec>> dto) {
        val tuple2 = dto.getData();
        val scoreFileSpec = tuple2.getT2();
        val objectId = scoreFileSpec.getObjectId();
        val uploadId = scoreFileSpec.getUploadId();
        val etagOrMd5 = tuple2.getT1();

        // The finalize step in score requires finalizing each file part and then the whole upload
        // we only have one file part, so we finalize the part and upload one after the other

        // finialize part publisher
        val finalizePartUrl = scoreRootUrl + "/upload/" + objectId + "/parts?" +
                                      "uploadId=" + uploadId +
                                      "&etag=" + etagOrMd5 +
                                      "&md5=" + etagOrMd5 +
                                      // we expect only one file part
                                      "&partNumber=1";
        val finalizeUploadPart = WebClient
                                         .create(finalizePartUrl)
                                         .post()
                                         .header("Authorization", "Bearer " + token)
                                         .retrieve()
                                         .toBodilessEntity();


        val finalizeUploadUrl = scoreRootUrl + "/upload/" + objectId + "?" +
                                        "uploadId=" + uploadId;
        val finalizeUpload = WebClient
                                     .create(finalizeUploadUrl)
                                     .post()
                                     .header("Authorization", "Bearer " + token)
                                     .retrieve()
                                     .toBodilessEntity();

        return finalizeUploadPart
                       .then(finalizeUpload)
                       .map(Objects::toString)
                       .map(str -> updateData(dto, str))
                       .log();
    }

    private Mono<String> publishAnalysis(PipeDTO<String> dto) {
        val analysisId = dto.getAnalysisId();
        val studyId = dto.getStudyId();

        val url = songRootUrl + "/studies/" + studyId + "/analysis/publish/" + analysisId +
                          "?ignoreUndefinedMd5=true"; // TODO - see if song client sets this to false
        return WebClient
                       .create(url)
                       .put()
                       .header("Authorization", "Bearer " + token)
                       .retrieve()
                       .toBodilessEntity()
                       .map(Objects::toString)
                       .log();
    }

    private static PipeDTO<Void> createPipeDto(String analysisId, String studyId) {
        return PipeDTO.<Void>builder().analysisId(analysisId).studyId(studyId).build();
    }

    private static <E> PipeDTO<E> updateData(PipeDTO<?> dto, E data) {
        return PipeDTO.<E>builder().analysisId(dto.getAnalysisId()).studyId(dto.getStudyId()).data(data).build();
    }
    
    @Builder
    @Value
    static class PipeDTO<T> {
        @NonNull String analysisId;
        @NonNull String studyId;
        T data;
    }
}
