package org.cancogenvirusseq.muse.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.StringWriter;
import java.util.Map;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.cancogenvirusseq.muse.components.SongScoreClient;
import org.cancogenvirusseq.muse.model.SubmissionEvent;
import org.cancogenvirusseq.muse.model.SubmissionFile;
import org.cancogenvirusseq.muse.model.TsvParserProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.util.function.Tuples;

@Slf4j
@Service
@RequiredArgsConstructor
public class SongScoreService {
  final SongScoreClient songScoreClient;
  final TsvParserProperties tsvParserProperties;

  private static final String SAMPLE_ID_HEADER = "submitterSampleId";
  private static final String STUDY_ID_HEADER = "studyId";

  private Sinks.Many<SubmissionEvent> sink = Sinks.many().unicast().onBackpressureBuffer();

  private Disposable songScoreProcessor;

  @PostConstruct
  public void init() {
    songScoreProcessor = createSongScoreProcessor();
  }

  @Bean
  public Sinks.Many<SubmissionEvent> songScoreServiceSink() {
    return sink;
  }

  private Disposable createSongScoreProcessor() {
    return sink.asFlux()
        .flatMap(
            submissionEvent -> {
              val records = submissionEvent.getRecords();
              val map = submissionEvent.getSubmissionFileMap();

              return Flux.fromStream(
                  records
                      .parallelStream()
                      .map(
                          r -> {
                            val sampleId = r.get(SAMPLE_ID_HEADER);
                            val studyId = r.get(STUDY_ID_HEADER);
                            val submissionFile = map.get(sampleId);
                            return Tuples.of(studyId, r, submissionFile);
                          }));
            })
        .flatMap(
            tuple3 -> {
              val submissionFile = tuple3.getT3();
              val partialPayloadStr = convertRecordToPayload(tuple3.getT2());
              val payload = fromJsonStr(partialPayloadStr);

              val filesNode = createFilesObject(submissionFile);
              payload.set("files", filesNode);

              log.info(payload.toPrettyString());
              // TODO - pass function for sink to db
              return songScoreClient.submitAndUpload(
                  tuple3.getT1(),
                  payload.toString(),
                  submissionFile.getContent(),
                  submissionFile.getFileMd5sum());
            })
         // TODO - onErrorContinue and sink to
        .subscribe();
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
