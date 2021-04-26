package org.cancogenvirusseq.muse.components;

import static java.util.stream.Collectors.toUnmodifiableList;
import static org.cancogenvirusseq.muse.model.tsv_parser.InvalidField.Reason.EXPECTING_NUMBER_TYPE;
import static org.cancogenvirusseq.muse.model.tsv_parser.InvalidField.Reason.NOT_ALLOWED_TO_BE_EMPTY;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.math.NumberUtils;
import org.cancogenvirusseq.muse.config.MuseAppConfig;
import org.cancogenvirusseq.muse.exceptions.submission.InvalidFieldsException;
import org.cancogenvirusseq.muse.exceptions.submission.InvalidHeadersException;
import org.cancogenvirusseq.muse.model.tsv_parser.InvalidField;
import org.cancogenvirusseq.muse.model.tsv_parser.TsvFieldSchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TsvParser {
  private final ImmutableList<TsvFieldSchema> tsvFieldSchemas;
  private final ImmutableList<String> expectedTsvHeaders;

  @Autowired
  public TsvParser(MuseAppConfig config) {
    this.tsvFieldSchemas = config.getTsvFieldSchemas();
    this.expectedTsvHeaders =
        ImmutableList.copyOf(
            tsvFieldSchemas.stream().map(TsvFieldSchema::getName).collect(toUnmodifiableList()));
  }

  public TsvParser(List<TsvFieldSchema> tsvFieldSchemas) {
    this.tsvFieldSchemas = ImmutableList.copyOf(tsvFieldSchemas);
    this.expectedTsvHeaders =
        ImmutableList.copyOf(
            tsvFieldSchemas.stream().map(TsvFieldSchema::getName).collect(toUnmodifiableList()));
  }

  @SneakyThrows
  public Stream<Map<String, String>> parseAndValidateTsvStrToFlatRecords(String s) {
    log.info("Parsing TSV into flat records");
    val lines = s.split("\n");
    val strTsvHeaders = List.of(lines[0].trim().split("\t"));

    val headerChkResult = checkHeaders(expectedTsvHeaders, strTsvHeaders);
    if (headerChkResult.isInvalid()) {
      throw new InvalidHeadersException(headerChkResult.missing, headerChkResult.unknown);
    }

    // parse records and run validation pipeline
    val records =
        parse(lines)
            .map(this::checkRequireNotEmpty)
            .map(this::checkValueTypes)
            .collect(toUnmodifiableList());

    if (hasAnyInvalidRecord(records)) {
      throw new InvalidFieldsException(getAllInvalidFieldErrors(records));
    }

    log.info("Parsed TSV successfully!");
    return records.stream().map(Record::getStringStringMap);
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

  private Stream<Record> parse(String[] lines) {
    val headers = lines[0].trim().split("\t");
    return IntStream.range(1, lines.length)
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
                val value = i >= data.length ? "" : data[i];
                record.put(headers[i], cleanup(value));
              }
              return new Record(j, record, new ArrayList<>());
            })
        .filter(this::recordNotEmpty);
  }

  private Record checkRequireNotEmpty(Record record) {
    tsvFieldSchemas.forEach(
        s -> {
          val fieldName = s.getName();
          val value = record.getStringStringMap().get(fieldName);
          if (s.isRequireNotEmpty() && isEmpty(value)) {
            record.addFieldError(fieldName, NOT_ALLOWED_TO_BE_EMPTY, record.getIndex());
          }
        });

    return record;
  }

  private Record checkValueTypes(Record record) {
    tsvFieldSchemas.forEach(
        s -> {
          val fieldName = s.getName();
          val value = record.getStringStringMap().get(fieldName);
          if (s.getValueType().equals(TsvFieldSchema.ValueType.number)
              && isNotEmpty(value) // ignore empty because it's checked before
              && isNotNumber(value)) {
            record.addFieldError(fieldName, EXPECTING_NUMBER_TYPE, record.getIndex());
          }
        });

    return record;
  }

  private Boolean hasAnyInvalidRecord(List<Record> records) {
    return records.stream().anyMatch(Record::hasFieldErrors);
  }

  private List<InvalidField> getAllInvalidFieldErrors(List<Record> records) {
    return records.stream()
        .map(Record::getFieldErrors)
        .flatMap(List::stream)
        .collect(toUnmodifiableList());
  }

  private Boolean recordNotEmpty(Record recordsDto) {
    return !recordsDto.getStringStringMap().values().stream()
        .allMatch(v -> v.trim().equalsIgnoreCase(""));
  }

  private static String cleanup(String rawValue) {
    return rawValue.replace("\r", "").replace("\n", "");
  }

  private static Boolean isEmpty(String value) {
    return value == null || value.trim().equalsIgnoreCase("");
  }

  private static Boolean isNotEmpty(String value) {
    return !isEmpty(value);
  }

  private static Boolean isNotNumber(String value) {
    return !NumberUtils.isCreatable(value);
  }

  @Value
  static class HeaderCheckResult {
    List<String> missing;
    List<String> unknown;

    Boolean isInvalid() {
      return !missing.isEmpty() || !unknown.isEmpty();
    }
  }

  @Value
  static class Record {
    Integer index;
    Map<String, String> stringStringMap;
    List<InvalidField> fieldErrors;

    public void addFieldError(String fieldName, InvalidField.Reason reason, Integer index) {
      fieldErrors.add(new InvalidField(fieldName, reason, index));
    }

    public Boolean hasFieldErrors() {
      return fieldErrors.size() > 0;
    }
  }
}
