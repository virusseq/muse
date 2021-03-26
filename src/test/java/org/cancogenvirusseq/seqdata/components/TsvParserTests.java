package org.cancogenvirusseq.seqdata.components;

import java.util.Map;

import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

public class TsvParserTests {

  @Test
  void testTsvParsedToMap() {
    val tsvStr =
        "studyId\tname\tsubmitterSampleId\n" + "COVID-PR\tconsensus_sequence\tQc-L00244359\n";

    val expect =
        Map.of(
            "name", "consensus_sequence",
                "submitterSampleId", "Qc-L00244359",
                "studyId", "COVID-PR");

    val source = TsvParser.parseTsvStrToFlatRecords(tsvStr);

    StepVerifier.create(source).expectNextMatches(record -> record.equals(expect)).verifyComplete();
  }
}
