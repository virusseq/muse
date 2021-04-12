package org.cancogenvirusseq.muse.components;

import static java.util.stream.Collectors.toUnmodifiableList;
import static org.cancogenvirusseq.muse.utils.AnalysisPayloadUtils.getFirstSubmitterSampleId;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringSubstitutor;
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

    val result =
        records.stream()
            .reduce(
                new MapperReduceResult(),
                accumulator(filesMap, config.getPayloadJsonTemplate()),
                combiner());

    val usedSampleIds = result.getUsedSampleIds();
    val sampleIdInRecordMissingFile = result.getSampleIdInRecordMissingFile();

    val sampleIdInFileMissingInTsv =
        filesMap.keySet().stream()
            .filter(s -> !usedSampleIds.contains(s))
            .collect(toUnmodifiableList());

    if (sampleIdInFileMissingInTsv.size() > 0 || sampleIdInRecordMissingFile.size() > 0) {
      throw new PayloadFileMapperException(sampleIdInFileMissingInTsv, sampleIdInRecordMissingFile);
    }

    return result.getRecordsMapped();
  }

  private static BiFunction<MapperReduceResult, Map<String, String>, MapperReduceResult>
      accumulator(ConcurrentHashMap<String, SubmissionFile> filesMap, String payloadTemplate) {
    return (acc, r) -> {
      val partialPayloadStr = convertRecordToPayload(r, payloadTemplate);
      val payload = fromJsonStr(partialPayloadStr);
      val sampleId = getFirstSubmitterSampleId(payload);

      val submissionFile = filesMap.get(sampleId);
      if (submissionFile == null) {
        acc.getSampleIdInRecordMissingFile().add(sampleId);
        return acc;
      }

      acc.getUsedSampleIds().add(sampleId);
      val filesNode = createFilesObject(submissionFile);
      payload.set("files", filesNode);
      acc.getRecordsMapped().add(Tuples.of(payload, submissionFile));

      return acc;
    };
  }

  private static BinaryOperator<MapperReduceResult> combiner() {
    return (first, second) -> {
      first.getRecordsMapped().addAll(second.getRecordsMapped());
      first.getUsedSampleIds().addAll(second.getUsedSampleIds());
      first.getSampleIdInRecordMissingFile().addAll(second.getSampleIdInRecordMissingFile());
      return first;
    };
  }

  @SneakyThrows
  private static ObjectNode fromJsonStr(String jsonStr) {
    return new ObjectMapper().readValue(jsonStr, ObjectNode.class);
  }

  private static String convertRecordToPayload(
      Map<String, String> valuesMap, String payloadTemplate) {
    val sub = new StringSubstitutor(valuesMap);
    // throw error if valuesMap is missing template values in payloadTemplate
    sub.setEnableUndefinedVariableException(true);
    return sub.replace(payloadTemplate);
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

  @Data
  @NoArgsConstructor
  static class MapperReduceResult {
    List<String> usedSampleIds = new ArrayList<>();
    List<String> sampleIdInRecordMissingFile = new ArrayList<>();
    List<Tuple2<ObjectNode, SubmissionFile>> recordsMapped = new ArrayList<>();
  }
}
