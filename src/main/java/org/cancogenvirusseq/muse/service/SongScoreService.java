package org.cancogenvirusseq.muse.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.PostConstruct;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.cancogenvirusseq.muse.components.SongScoreClient;
import org.cancogenvirusseq.muse.model.SubmissionEvent;
import org.cancogenvirusseq.muse.model.SubmissionFile;
import org.cancogenvirusseq.muse.model.TsvParserProperties;
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
  final TsvParserProperties tsvParserProperties;
  final UploadRepository repo;

  private static final String SAMPLE_ID_HEADER = "submitterSampleId";
  private static final String STUDY_ID_HEADER = "studyId";

  private Sinks.Many<SubmissionEvent> sink = Sinks.many().unicast().onBackpressureBuffer();

  private Disposable submitUploadDisposable;

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
        .flatMap(this::extractPayloadUploadAndSubFileFromEvent)
        .flatMap(this::submitAndUploadToSongScore)
        .subscribe();
  }

  public Flux<Tuple3<String, Upload, SubmissionFile>> extractPayloadUploadAndSubFileFromEvent(
      SubmissionEvent submissionEvent) {
    val records = submissionEvent.getRecords();
    val map = submissionEvent.getSubmissionFilesMap();

    return Flux.fromStream(
        records
            .parallelStream()
            .map(
                r -> {
                  val sampleId = r.get(SAMPLE_ID_HEADER);
                  val studyId = r.get(STUDY_ID_HEADER);
                  val submissionFile = map.get(sampleId);

                  val upload =
                      Upload.builder()
                          .studyId(studyId)
                          .submitterSampleId(sampleId)
                          .submissionId(submissionEvent.getSubmissionId())
                          .userId(submissionEvent.getUserId())
                          .status(UploadStatus.QUEUED)
                          .originalFilePair(List.of(submissionFile.getFileName()))
                          .build();

                  val partialPayloadStr = convertRecordToPayload(r);
                  val payload = fromJsonStr(partialPayloadStr);

                  val filesNode = createFilesObject(submissionFile);
                  payload.set("files", filesNode);

                  log.info(payload.toPrettyString());
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
            (t) -> {
              t.printStackTrace();
              upload.setStatus(UploadStatus.ERROR);
              upload.setError(t.getMessage());
              return repo.save(upload);
            });
  }

  @SneakyThrows
  private ObjectNode fromJsonStr(String jsonStr) {
    return new ObjectMapper().readValue(jsonStr, ObjectNode.class);
  }

  private String convertRecordToPayload(Map<String, String> valuesMap) {
    val context = new VelocityContext();
    valuesMap.forEach(context::put);
    val writer = new StringWriter();
    Velocity.evaluate(context, writer, "", tsvParserProperties.getPayloadJsonTemplate());
    return writer.toString();
  }

  private static JsonNode createFilesObject(SubmissionFile submissionFile) {
    val filesArray = JsonNodeFactory.instance.arrayNode(1);
    val fileObj = JsonNodeFactory.instance.objectNode();

    fileObj.put("fileName", submissionFile.getFileName());
    fileObj.put("fileSize", submissionFile.getFileSize());
    fileObj.put("fileMd5sum", submissionFile.getFileMd5sum());
    fileObj.put("fileAccess", submissionFile.getFileAccess());
    fileObj.put("fileType", submissionFile.getFileType());
    fileObj.put("dataType", submissionFile.getDataType());

    filesArray.insert(0, fileObj);
    return filesArray;
  }
}
