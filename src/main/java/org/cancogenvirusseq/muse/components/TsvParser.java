package org.cancogenvirusseq.muse.components;

import static java.util.stream.Collectors.toUnmodifiableList;
import static org.apache.commons.lang.StringUtils.isNumeric;
import static org.cancogenvirusseq.muse.model.tsv_parser.InvalidField.Reason.EXPECTING_NUMBER_TYPE;

import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.cancogenvirusseq.muse.config.MuseAppConfig;
import org.cancogenvirusseq.muse.exceptions.submission.InvalidFieldsException;
import org.cancogenvirusseq.muse.exceptions.submission.MissingHeadersException;
import org.cancogenvirusseq.muse.model.tsv_parser.InvalidField;
import org.cancogenvirusseq.muse.model.tsv_parser.TsvFieldSchema;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TsvParser {
  private final MuseAppConfig config;

  @SneakyThrows
  public Stream<Map<String, String>> parseAndValidateTsvStrToFlatRecords(String s) {
    val lines = s.split("\n");
    val strTsvHeaders = List.of(lines[0].trim().split("\t"));

    val headerChkResult = checkHeaders(config.getExpectedTsvHeaders(), strTsvHeaders);
    if (headerChkResult.isInvalid()) {
      throw new MissingHeadersException(headerChkResult.missing, headerChkResult.unknown);
    }

    // TODO - don't pass header in lines
    val result = parseAndValidate(lines, strTsvHeaders.toArray(String[]::new));
    if (result.getInvalidFields().size() > 0) {
      throw new InvalidFieldsException(result.getInvalidFields());
    }

    return result.getRecords().stream();
  }

  private HeaderCheckResult checkHeaders(List<String> expectedHeaders, List<String> actualHeaders) {
    val missingHeaders =
        expectedHeaders.stream()
            .filter(h -> !actualHeaders.contains(h))
            .collect(toUnmodifiableList());

    val unknownHeaders =
        actualHeaders.stream()
            .filter(h -> !expectedHeaders.contains(h))
            .collect(toUnmodifiableList());

    return new HeaderCheckResult(missingHeaders, unknownHeaders);
  }

  private ParserValidResult parseAndValidate(String[] lines, String[] headers) {
    val fieldErrors = new ArrayList<InvalidField>();
    val records =
        // TODO - start from 0 after lines doesn't have headers in it
        IntStream.range(1, lines.length)
            .parallel()
            .filter(
                j -> {
                  val line = lines[j];
                  return line != null && !line.trim().equals("");
                })
            .mapToObj(
                j -> {
                  val line = lines[j];
                  val data = line.split("\t");

                  Map<String, String> record = new HashMap<>();
                  for (int i = 0; i < headers.length; ++i) {
                    record.put(headers[i], i > data.length ? "" : data[i]);
                  }

                  // collect field errors for record
                  fieldErrors.addAll(checkValueTypes(record, j));

                  return record;
                })
            .collect(toUnmodifiableList());

    return new ParserValidResult(records, fieldErrors);
  }

  private List<InvalidField> checkValueTypes(Map<String, String> record, Integer index) {
    return config.getTsvFieldSchemas().stream()
        .map(
            s -> {
              val expectedValueType = s.getValueType();
              val fieldName = s.getName();
              val value = record.get(s.getName());

              if (expectedValueType.equals(TsvFieldSchema.ValueType.number) && !isNumeric(value)) {
                return new InvalidField(fieldName, EXPECTING_NUMBER_TYPE, index);
              }
              return null;
            })
        .filter(Objects::nonNull)
        .collect(toUnmodifiableList());
  }

  @Value
  static class ParserValidResult {
    List<Map<String, String>> records;
    List<InvalidField> invalidFields;
  }

  @Value
  static class HeaderCheckResult {
    List<String> missing;
    List<String> unknown;

    Boolean isInvalid() {
      return !missing.isEmpty() || !unknown.isEmpty();
    }
  }
}
