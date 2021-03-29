package org.cancogenvirusseq.muse.components;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.cancogenvirusseq.muse.model.FileMeta;
import reactor.core.publisher.Flux;

@RequiredArgsConstructor
public class FilePayloadMapper implements Function<Flux<ObjectNode>, Flux<ObjectNode>> {
  final ConcurrentHashMap<String, FileMeta> repo;

  public Flux<ObjectNode> apply(Flux<ObjectNode> partialPayloadFlux) {
    return partialPayloadFlux
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
        .filter(Objects::nonNull);
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
