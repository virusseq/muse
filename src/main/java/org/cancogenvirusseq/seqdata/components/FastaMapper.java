package org.cancogenvirusseq.seqdata.components;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class FastaMapper {
    final ConcurrentHashMap<String, String> repo;

    public FastaMapper(String fastaFileContent) {
        this.repo = new ConcurrentHashMap<>();
        Arrays.stream(fastaFileContent.split(">")).forEach(f -> {
            if (f == null || f.trim().equals("")) return;
            val sampleId = f.split("/")[1];
            if (sampleId == null) return;
            // TODO find better implementation adding strings creates new string which is inefficient!
            this.repo.put(sampleId, ">" + f);
        });
    }

    public List<ObjectNode> apply(List<ObjectNode> partialPayloadFlux) {
        return partialPayloadFlux.stream().map(partialPayload -> {
            val sampleId = partialPayload.get("samples").get(0).get("submitterSampleId").asText();
            val fileContent = repo.get(sampleId);

            if (sampleId == null || fileContent == null) {
                return null;
            }
                val fileMd5sum = md5(fileContent);
                val fileName = sampleId + ".v0.fasta";
                val fileSize = fileContent.length();
                val filesNode = createFilesObject(fileName, fileSize, fileMd5sum);

                partialPayload.set("files", filesNode);

                return partialPayload;
        }).filter(Objects::nonNull).collect(Collectors.toUnmodifiableList());
    }

    @SneakyThrows
    public static HashCode md5(String input) {
        val hashFunction = Hashing.md5();
        return hashFunction.hashString(input, StandardCharsets.UTF_8);
    }

    private static JsonNode createFilesObject(String fileName, Integer fileSize, HashCode fileMd5Sum) {
        val filesArray = JsonNodeFactory.instance.arrayNode(1);
        val fileObj = JsonNodeFactory.instance.objectNode();


        fileObj.put("fileName", fileName);
        fileObj.put("fileSize", fileSize);
        fileObj.put("fileMd5sum", fileMd5Sum.toString());

        // All files are open
        fileObj.put("fileAccess", "OPEN");

        // TBD
        fileObj.put("fileType", "FASTA");
        fileObj.put("dataType", "FASTA");
        fileObj.with("info").put("data_category", "FASTA");

        filesArray.insert(0, fileObj);

        return filesArray;
    }
}
