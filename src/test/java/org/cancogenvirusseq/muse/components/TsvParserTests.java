package org.cancogenvirusseq.muse.components;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import lombok.val;
import org.cancogenvirusseq.muse.model.tsv_parser.TsvFieldSchema;
import org.junit.jupiter.api.Test;

public class TsvParserTests {

  @Test
  @SneakyThrows
  void testTsvStrParsedToRecords() {
    val fields =
        ImmutableList.of(
            new TsvFieldSchema("submitterId", TsvFieldSchema.ValueType.string),
            new TsvFieldSchema("name", TsvFieldSchema.ValueType.string),
            new TsvFieldSchema("age", TsvFieldSchema.ValueType.string));

    val parser = new TsvParser(fields);

    val tsvStr = "age\tname\tsubmitterId\n" + "123\tconsensus_sequence\tQc-L00244359\n";

    val expected =
        List.of(Map.of("submitterId", "Qc-L00244359", "name", "consensus_sequence", "age", "123"));

    val source =
        parser.parseAndValidateTsvStrToFlatRecords(tsvStr).collect(Collectors.toUnmodifiableList());

    assertThat(source).hasSameElementsAs(expected);
  }
}
