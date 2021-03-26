package org.cancogenvirusseq.seqdata.components;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import org.cancogenvirusseq.seqdata.model.FileMeta;

@RequiredArgsConstructor
public class FilePayloadMapper {
  final ConcurrentHashMap<String, FileMeta> repo;


  public List<ObjectNode> apply(List<ObjectNode> partialPayloadFlux) {
    return partialPayloadFlux.stream()
        .map(
            partialPayload -> {
              val sampleId = partialPayload.get("samples").get(0).get("submitterSampleId").asText();
              val fileMeta = repo.get(sampleId);

              if (sampleId == null || fileMeta == null) {
                return null;
              }
              val filesNode = createFilesObject(fileMeta);

              partialPayload.set("files", filesNode);
              return partialPayload;
            })
        .filter(Objects::nonNull)
        .collect(Collectors.toUnmodifiableList());
  }

  @SneakyThrows
  public static HashCode md5(String input) {
    val hashFunction = Hashing.md5();
    return hashFunction.hashString(input, StandardCharsets.UTF_8);
  }

  private static JsonNode createFilesObject(FileMeta fileMeta) {
    val filesArray = JsonNodeFactory.instance.arrayNode(1);
    val fileObj = JsonNodeFactory.instance.objectNode();

    fileObj.put("fileName", fileMeta.getFileName());
    fileObj.put("fileSize", fileMeta.getFileSize());
    fileObj.put("fileMd5sum", fileMeta.getFileMd5sum());
    fileObj.put("fileAccess", fileMeta.getFileAccess());
    fileObj.put("fileType", fileMeta.getFileType());
    fileObj.put("dataType", fileMeta.getDataType());
    fileObj.with("info").put("data_category", fileMeta.getDataCategory());

    filesArray.insert(0, fileObj);
    return filesArray;
  }
}
