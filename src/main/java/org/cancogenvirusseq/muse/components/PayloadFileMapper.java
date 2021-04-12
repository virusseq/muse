package org.cancogenvirusseq.muse.components;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringSubstitutor;
import org.cancogenvirusseq.muse.config.MuseAppConfig;
import org.cancogenvirusseq.muse.exceptions.submission.PayloadFileMapperException;
import org.cancogenvirusseq.muse.model.SubmissionBundle;
import org.cancogenvirusseq.muse.model.SubmissionFile;
import org.cancogenvirusseq.muse.model.SubmissionRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;

import static java.util.stream.Collectors.toUnmodifiableList;
import static org.cancogenvirusseq.muse.utils.AnalysisPayloadUtils.getFirstSubmitterSampleId;

@Slf4j
@Component
@RequiredArgsConstructor
public class PayloadFileMapper {
  private final MuseAppConfig config;

  @SneakyThrows
  public List<SubmissionRequest> submissionBundleToSubmissionRequests(
      SubmissionBundle submissionBundle) {
    val result =
        submissionBundle.getRecords().stream()
            .reduce(
                new MapperReduceResult(),
                accumulator(submissionBundle.getFiles(), config.getPayloadJsonTemplate()),
                combiner());

    val usedSampleIds = result.getUsedSampleIds();
    val sampleIdInRecordMissingFile = result.getSampleIdInRecordMissingFile();

    val sampleIdInFileMissingInTsv =
        submissionBundle.getFiles().keySet().stream()
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
      acc.getRecordsMapped().add(new SubmissionRequest(payload, submissionFile));

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
    List<SubmissionRequest> recordsMapped = new ArrayList<>();
  }

}
