package org.cancogenvirusseq.muse.components;

import static java.util.stream.Collectors.toUnmodifiableList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import lombok.val;
import org.cancogenvirusseq.muse.exceptions.submission.InvalidFieldsException;
import org.cancogenvirusseq.muse.exceptions.submission.InvalidHeadersException;
import org.cancogenvirusseq.muse.model.tsv_parser.InvalidField;
import org.cancogenvirusseq.muse.model.tsv_parser.TsvFieldSchema;
import org.junit.jupiter.api.Test;

public class TsvParserTests {

  private static final ImmutableList<TsvFieldSchema> TSV_SCHEMA =
      ImmutableList.of(
          new TsvFieldSchema("submitterId", TsvFieldSchema.ValueType.string),
          new TsvFieldSchema("name", TsvFieldSchema.ValueType.string),
          new TsvFieldSchema("age", TsvFieldSchema.ValueType.number));

  @Test
  @SneakyThrows
  void testTsvStrParsedToRecords() {
    val tsvStr = "age\tname\tsubmitterId\n" + "123\tconsensus_sequence\tQc-L00244359\n";
    val expected =
        List.of(Map.of("submitterId", "Qc-L00244359", "name", "consensus_sequence", "age", "123"));

    val parser = new TsvParser(TSV_SCHEMA);
    val actual = parser.parseAndValidateTsvStrToFlatRecords(tsvStr).collect(toUnmodifiableList());

    assertThat(actual).hasSameElementsAs(expected);
  }

  @Test
  void testErrorOnInvalidHeaders() {
    val tsvStr = "agee\tname\tsubmitterId\n" + "123\tconsensus_sequence\tQc-L00244359\n";

    val parser = new TsvParser(TSV_SCHEMA);
    val thrown =
        assertThrows(
            InvalidHeadersException.class,
            () -> parser.parseAndValidateTsvStrToFlatRecords(tsvStr));

    assertThat(thrown.getMissingHeaders()).contains("age");
    assertThat(thrown.getUnknownHeaders()).contains("agee");
  }

  @Test
  void testErrorOnInvalidNumberType() {
    val tsvStr = "age\tname\tsubmitterId\n" + "onetwothree\tconsensus_sequence\tQc-L00244359\n";

    val parser = new TsvParser(TSV_SCHEMA);
    val thrown =
        assertThrows(
            InvalidFieldsException.class, () -> parser.parseAndValidateTsvStrToFlatRecords(tsvStr));

    val expectedInvalidField =
        new InvalidField("age", InvalidField.Reason.EXPECTING_NUMBER_TYPE, 1);

    assertThat(thrown.getInvalidFields()).contains(expectedInvalidField);
  }
}
