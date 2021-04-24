package org.cancogenvirusseq.muse.components;

import static java.lang.String.format;
import static java.util.stream.Collectors.toUnmodifiableList;
import static org.cancogenvirusseq.muse.utils.AnalysisPayloadUtils.getFirstSubmitterSampleId;
import static org.cancogenvirusseq.muse.utils.AnalysisPayloadUtils.getIsolate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.text.StringSubstitutor;
import org.apache.commons.text.lookup.StringLookup;
import org.cancogenvirusseq.muse.config.MuseAppConfig;
import org.cancogenvirusseq.muse.exceptions.submission.PayloadFileMapperException;
import org.cancogenvirusseq.muse.model.SubmissionBundle;
import org.cancogenvirusseq.muse.model.SubmissionFile;
import org.cancogenvirusseq.muse.model.SubmissionRequest;
import org.springframework.stereotype.Component;

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
                accumulator(submissionBundle, config.getPayloadJsonTemplate()),
                combiner());

    val isolateInRecordMissingFile = result.getIsolateInRecordMissingInFile();

    val isolateInFileMissingInTsv =
        submissionBundle.getFiles().keySet().stream()
            .filter(s -> !result.getUsedIsolates().contains(s))
            .collect(toUnmodifiableList());

    if (isolateInFileMissingInTsv.size() > 0 || isolateInRecordMissingFile.size() > 0) {
      throw new PayloadFileMapperException(isolateInFileMissingInTsv, isolateInRecordMissingFile);
    }

    return result.getRecordsMapped();
  }

  private static BiFunction<MapperReduceResult, Map<String, String>, MapperReduceResult>
      accumulator(SubmissionBundle submissionBundle, String payloadTemplate) {
    return (acc, r) -> {
      val payload = convertRecordToPayload(r, payloadTemplate);
      val isolate = getIsolate(payload);
      val submissionFile = submissionBundle.getFiles().get(isolate);

      if (submissionFile == null) {
        acc.getIsolateInRecordMissingInFile().add(isolate);
        return acc;
      }

      val sampleFileName =
          format("%s%s", getFirstSubmitterSampleId(payload), submissionFile.getFileExtension());

      payload.set("files", createFilesObject(submissionFile, sampleFileName));

      acc.getUsedIsolates().add(isolate);
      acc.getRecordsMapped()
          .add(
              new SubmissionRequest(
                  payload, submissionFile, submissionBundle.getOriginalFileNames()));

      return acc;
    };
  }

  private static BinaryOperator<MapperReduceResult> combiner() {
    return (first, second) -> {
      first.getRecordsMapped().addAll(second.getRecordsMapped());
      first.getUsedIsolates().addAll(second.getUsedIsolates());
      first.getIsolateInRecordMissingInFile().addAll(second.getIsolateInRecordMissingInFile());
      return first;
    };
  }

  @SneakyThrows
  private static ObjectNode convertRecordToPayload(
      Map<String, String> valuesMap, String payloadTemplate) {
    StringLookup lookupFunc =
        key -> {
          val value = valuesMap.getOrDefault(key, "");

          // value is mapped to json value by these rules
          if (NumberUtils.isCreatable(value)) {
            // numeric no need to append quotes
            return value;
          } else if (value.trim().equals("")) {
            // empty string map to null value
            return "null";
          } else {
            // for string append double quotes
            return format("\"%s\"", value);
          }
        };

    val sub = new StringSubstitutor(lookupFunc);
    // throw error if valuesMap is missing template values in payloadTemplate
    sub.setEnableUndefinedVariableException(true);

    return new ObjectMapper().readValue(sub.replace(payloadTemplate), ObjectNode.class);
  }

  private static JsonNode createFilesObject(SubmissionFile submissionFile, String fileName) {
    val filesArray = JsonNodeFactory.instance.arrayNode(1);
    val fileObj = JsonNodeFactory.instance.objectNode();

    fileObj.put("fileName", fileName);
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
    List<String> usedIsolates = new ArrayList<>();
    List<String> isolateInRecordMissingInFile = new ArrayList<>();
    List<SubmissionRequest> recordsMapped = new ArrayList<>();
  }
}
