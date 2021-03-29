package org.cancogenvirusseq.seqdata.components;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import lombok.SneakyThrows;
import lombok.val;
import org.cancogenvirusseq.seqdata.model.TsvFieldSchema;
import org.cancogenvirusseq.seqdata.model.TsvParserProperties;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

public class TsvParserTests {

  @Test
  @SneakyThrows
  void testTsvStrParsedToPayload() {
    val fields = ImmutableList.of(new TsvFieldSchema("studyId", "string"),
                                                            new TsvFieldSchema("submitterSampleId", "string"),
                                                            new TsvFieldSchema("name", "string"));
    val jsonTemplateStr = "{\n" +
                          "    \"analysisType\": {\n" +
                          "        \"name\": \"$name\"\n" +
                          "    },\n" +
                          "    \"studyId\": \"$studyId\",\n" +
                          "    \"samples\": [{\"submitterSampleId\": \"$submitterSampleId\"}]" +
                          "}";

    val tsvParserProperties = new TsvParserProperties(fields, jsonTemplateStr);
    val parser = new TsvParser(tsvParserProperties);

    val tsvStr =
            "studyId\tname\tsubmitterSampleId\n" +
            "COVID-PR\tconsensus_sequence\tQc-L00244359\n";

    val expectedJsonStr =  "{\n" +
                              "    \"analysisType\": {\n" +
                              "        \"name\": \"consensus_sequence\"\n" +
                              "    },\n" +
                              "    \"studyId\": \"COVID-PR\",\n" +
                              "    \"samples\": [{\"submitterSampleId\": \"Qc-L00244359\"}]" +
                              "}";
    val expectedObjectNode = new ObjectMapper().readValue(expectedJsonStr, ObjectNode.class);

    val source = parser.parseTsvStrToAnalysisPayloads(tsvStr);

    StepVerifier.create(source)
            .expectNextMatches(actualNode -> actualNode.equals(expectedObjectNode)).verifyComplete();
  }

  @Test
  void testTsvParsedToMap() {
    val tsvStr =
        "studyId\tname\tsubmitterSampleId\n" + "COVID-PR\tconsensus_sequence\tQc-L00244359\n";

    val expect =
        Map.of(
            "name",
            "consensus_sequence",
            "submitterSampleId",
            "Qc-L00244359",
            "studyId",
            "COVID-PR");

    val source = TsvParser.parseTsvStrToFlatRecords(tsvStr);

    StepVerifier.create(source).expectNextMatches(record -> record.equals(expect)).verifyComplete();
  }
}
