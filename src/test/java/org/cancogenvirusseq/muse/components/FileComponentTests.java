package org.cancogenvirusseq.muse.components;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.SneakyThrows;
import lombok.val;
import org.cancogenvirusseq.muse.model.SubmissionFile;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.cancogenvirusseq.muse.components.FastaFileProcessor.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class FileComponentTests {
  String sampleId1 = "sam1";
  String sampleId2 = "sam2";

  SubmissionFile file1Meta =
      SubmissionFile.builder()
          .fileName(sampleId1 + FASTA_FILE_EXTENSION)
          .fileSize(26)
          .fileMd5sum("f433d470a7bacc3bdcdafeb1a4b4d758")
          .content(">ABCD/sam1/ddd/erd \nCTGA \n")
          .fileType(FASTA_TYPE)
          .dataCategory(FASTA_TYPE)
          .dataType(FASTA_TYPE)
          .build();

  SubmissionFile file2Meta =
      SubmissionFile.builder()
          .fileName(sampleId2 + FASTA_FILE_EXTENSION)
          .fileSize(23)
          .fileMd5sum("eecf3de7e1136d99fffdd781d76bc81a")
          .content(">EFG/sam2/ddd/erd \nATGC")
          .fileType(FASTA_TYPE)
          .dataCategory(FASTA_TYPE)
          .dataType(FASTA_TYPE)
          .build();

  ConcurrentHashMap<String, SubmissionFile> sampleIdToFileMeta =
      new ConcurrentHashMap<>(Map.of(sampleId1, file1Meta, sampleId2, file2Meta));

  @Test
  void testWriteToFileAndMeta() {
    val fastaFile = ">ABCD/sam1/ddd/erd \n" + "CTGA \n" + ">EFG/sam2/ddd/erd \n" + "ATGC";

    val fileMetaToSampleIdMap = processFileStrContent(fastaFile);

    assertEquals(sampleIdToFileMeta, fileMetaToSampleIdMap);
  }

  @Test
  @SneakyThrows
  void testPartialPayloadsMappedToFiles() {
    // todo: please fix with content and look at newlines
    val partialPayload1Str = "{ \"samples\": [ {\"submitterSampleId\": \"sam1\"}] }";
    val partialPayload2Str = "{ \"samples\": [ {\"submitterSampleId\": \"sam2\"}] }";

    val finalPayload1Str =
        "{ \"samples\": [ {\"submitterSampleId\": \"sam1\"}], "
            + "\"files\":[{\"fileName\":\"sam1.v0.fasta\",\"fileSize\":26,\"fileMd5sum\":\"f433d470a7bacc3bdcdafeb1a4b4d758\",\"fileAccess\":\"OPEN\",\"fileType\":\"FASTA\",\"dataType\":\"FASTA\",\"info\":{\"data_category\":\"FASTA\"}}]"
            + "}"
            + "";
    val finalPayload2Str =
        "{ \"samples\": [ {\"submitterSampleId\": \"sam2\"}], "
            + "\"files\":[{\"fileName\":\"sam2.v0.fasta\",\"fileSize\":23,\"fileMd5sum\":\"eecf3de7e1136d99fffdd781d76bc81a\",\"fileAccess\":\"OPEN\",\"fileType\":\"FASTA\",\"dataType\":\"FASTA\",\"info\":{\"data_category\":\"FASTA\"}}]"
            + "}"
            + "";

    val mapper = new ObjectMapper();
    val partialPayload1 = mapper.readValue(partialPayload1Str, ObjectNode.class);
    val partialPayload2 = mapper.readValue(partialPayload2Str, ObjectNode.class);
    val finalPayload1 = mapper.readValue(finalPayload1Str, ObjectNode.class);
    val finalPayload2 = mapper.readValue(finalPayload2Str, ObjectNode.class);

    val fileMapper = new FilePayloadMapper(sampleIdToFileMeta);
    val source = fileMapper.apply(Flux.just(partialPayload1, partialPayload2));

    StepVerifier.create(source)
        .expectNextMatches(actual -> actual.equals(finalPayload1))
        .expectNextMatches(actual -> actual.equals(finalPayload2))
        .verifyComplete();
  }
}
