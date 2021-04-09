package org.cancogenvirusseq.muse.components;

import static java.util.stream.Collectors.toUnmodifiableList;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.StringWriter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.cancogenvirusseq.muse.config.MuseAppConfig;
import org.cancogenvirusseq.muse.exceptions.submission.PayloadFileMapperException;
import org.cancogenvirusseq.muse.model.SubmissionFile;
import org.springframework.stereotype.Component;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

@Slf4j
@Component
@RequiredArgsConstructor
public class PayloadFileMapper {
  private final MuseAppConfig config;

  @SneakyThrows
  public List<Tuple2<ObjectNode, SubmissionFile>> mapAllPayloadToSubmissionFile(
      Tuple2<ArrayList<Map<String, String>>, ConcurrentHashMap<String, SubmissionFile>> tuple2) {
    val records = tuple2.getT1();
    val filesMap = tuple2.getT2();

    val usedSampleIds = new ArrayList<String>();
    val sampleIdInRecordMissingFile = new ArrayList<String>();

    List<Tuple2<ObjectNode, SubmissionFile>> mapped =
        records
            .parallelStream()
            .map(
                r -> {
                  val partialPayloadStr = convertRecordToPayload(r);
                  val payload = fromJsonStr(partialPayloadStr);

                  val sampleId = payload.get("samples").get(0).get("submitterSampleId").asText();

                  val submissionFile = filesMap.get(sampleId);
                  if (submissionFile == null) {
                    sampleIdInRecordMissingFile.add(sampleId);
                    return null;
                  }
                  usedSampleIds.add(sampleId);

                  val filesNode = createFilesObject(submissionFile);
                  payload.set("files", filesNode);

                  return Tuples.of(payload, submissionFile);
                })
            .filter(Objects::nonNull)
            .collect(toUnmodifiableList());

    val sampleIdInFileMissingInTsv =
        filesMap.keySet().stream()
            .filter(s -> !usedSampleIds.contains(s))
            .collect(toUnmodifiableList());

    if (sampleIdInFileMissingInTsv.size() > 0 || sampleIdInRecordMissingFile.size() > 0) {
      throw new PayloadFileMapperException(sampleIdInFileMissingInTsv, sampleIdInRecordMissingFile);
    }

    return mapped;
  }

  @SneakyThrows
  private static ObjectNode fromJsonStr(String jsonStr) {
    return new ObjectMapper().readValue(jsonStr, ObjectNode.class);
  }

  private String convertRecordToPayload(Map<String, String> valuesMap) {
    val context = new VelocityContext();
    valuesMap.forEach(context::put);
    val writer = new StringWriter();
    Velocity.evaluate(context, writer, "", config.getPayloadJsonTemplate());
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
