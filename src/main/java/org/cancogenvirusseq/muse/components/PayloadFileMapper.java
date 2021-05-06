package org.cancogenvirusseq.muse.components;

import static java.lang.String.format;
import static java.util.stream.Collectors.toUnmodifiableList;
import static org.cancogenvirusseq.muse.utils.AnalysisPayloadUtils.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.text.StringSubstitutor;
import org.apache.commons.text.lookup.StringLookup;
import org.cancogenvirusseq.muse.config.MuseAppConfig;
import org.cancogenvirusseq.muse.exceptions.submission.FoundInvalidFilesException;
import org.cancogenvirusseq.muse.exceptions.submission.MissingDataException;
import org.cancogenvirusseq.muse.model.SubmissionBundle;
import org.cancogenvirusseq.muse.model.SubmissionFile;
import org.cancogenvirusseq.muse.model.UploadRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PayloadFileMapper {
  private final String payloadJsonTemplate;

  @Autowired
  public PayloadFileMapper(MuseAppConfig config) {
    this.payloadJsonTemplate = config.getPayloadJsonTemplate();
  }

  public PayloadFileMapper(String payloadJsonTemplate) {
    this.payloadJsonTemplate = payloadJsonTemplate;
  }

  @SneakyThrows
  public Map<String, UploadRequest> submissionBundleToSubmissionRequests(
      SubmissionBundle submissionBundle) {
    log.info("Mapping payloads to files");

    val invalidFiles = findFilesWithHeaderOnly(submissionBundle.getFiles());
    if (invalidFiles.size() > 0) {
      throw new FoundInvalidFilesException(invalidFiles);
    }

    val result =
        submissionBundle.getRecords().stream()
            .reduce(
                new MapperReduceResult(),
                accumulator(submissionBundle, payloadJsonTemplate),
                combiner());

    val isolateInRecordMissingFile = result.getIsolateInRecordMissingInFile();

    val isolateInFileMissingInTsv =
        submissionBundle.getFiles().keySet().stream()
            .filter(s -> !result.getUsedIsolates().contains(s))
            .collect(toUnmodifiableList());

    if (isolateInFileMissingInTsv.size() > 0 || isolateInRecordMissingFile.size() > 0) {
      throw new MissingDataException(isolateInFileMissingInTsv, isolateInRecordMissingFile);
    }

    log.info("Mapped all payloads to files");
    return result.getRecordsMapped();
  }

  private static List<String> findFilesWithHeaderOnly(
      ConcurrentHashMap<String, SubmissionFile> files) {
    return files.entrySet().stream()
        .filter(
            entry ->
                // add two for the ">" and "\n" that exist in header and not in isolate
                entry.getKey().length() + 2 == entry.getValue().getFileSize())
        .map(Map.Entry::getKey)
        .collect(Collectors.toUnmodifiableList());
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

      val submitterSampleId = getFirstSubmitterSampleId(payload);
      val sampleFileName = format("%s%s", submitterSampleId, submissionFile.getFileExtension());

      payload.set("files", createFilesObject(submissionFile, sampleFileName));

      val uploadRequest =
          UploadRequest.builder()
              .submitterSampleId(submitterSampleId)
              .studyId(getStudyId(payload))
              .record(payload)
              .submissionFile(submissionFile)
              .originalFileNames(
                  Stream.concat(
                          submissionBundle.getOriginalFileNames().stream()
                              .filter(filename -> filename.endsWith(".tsv")),
                          Stream.of(submissionFile.getSubmittedFileName()))
                      .collect(Collectors.toSet()))
              .build();

      acc.getUsedIsolates().add(isolate);
      acc.getRecordsMapped().put(uploadRequest.getCompositeId(), uploadRequest);

      return acc;
    };
  }

  private static BinaryOperator<MapperReduceResult> combiner() {
    return (first, second) -> {
      first.getRecordsMapped().putAll(second.getRecordsMapped());
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
    Map<String, UploadRequest> recordsMapped = new HashMap<>();
  }
}
