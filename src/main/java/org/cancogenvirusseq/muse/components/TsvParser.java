package org.cancogenvirusseq.muse.components;

import static java.util.stream.Collectors.toUnmodifiableList;
import static org.cancogenvirusseq.muse.model.tsv_parser.InvalidField.Reason.NOT_ALLOWED_TO_BE_EMPTY;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
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
    val lines = s.split("\n");
    val strTsvHeaders = List.of(lines[0].trim().split("\t"));

    val headerChkResult = checkHeaders(expectedTsvHeaders, strTsvHeaders);
    if (headerChkResult.isInvalid()) {
      throw new InvalidHeadersException(headerChkResult.missing, headerChkResult.unknown);
    }
    val result = parse(lines)
                         .map(this::checkRequireNotEmptyFields)
//                        .map(this::convertRecordValueTypes);
                         .collect(toUnmodifiableList());

    if (hasAnyInvalidRecord(result)) {
      throw new InvalidFieldsException(getAllInvalidFieldErrors(result));
    }

    return result.stream().map(RecordDTO::getRecord);
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

  private Stream<RecordDTO> parse(String[] lines) {
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
              return new RecordDTO(j, record, new ArrayList<>());
            })
        .filter(this::recordNotEmpty);
  }

  private RecordDTO checkRequireNotEmptyFields(RecordDTO recordDTO) {
    tsvFieldSchemas
             .forEach(s -> {
                 val fieldName = s.getName();
                 val value = recordDTO.getRecord().get(fieldName);
                 if (s.getRequireNotEmpty() && isEmpty(value)) {
                   recordDTO.addFieldError(fieldName, NOT_ALLOWED_TO_BE_EMPTY, recordDTO.getIndex());
                 }
             });

    return recordDTO;
  }

  private Boolean hasAnyInvalidRecord(List<RecordDTO> recordDTOs) {
    return recordDTOs.stream().anyMatch(RecordDTO::hasFieldErrors);
  }

  private List<InvalidField> getAllInvalidFieldErrors(List<RecordDTO> recordDTOS) {
    return recordDTOS.stream()
                   .map(RecordDTO::getFieldErrors)
                   .flatMap(List::stream)
                   .collect(toUnmodifiableList());
  }

  private Boolean recordNotEmpty(RecordDTO recordsDto) {
    return !recordsDto.getRecord().values().stream().allMatch(v -> v.trim().equalsIgnoreCase(""));
  }

  private static String cleanup(String rawValue) {
    return rawValue.replace("\r", "").replace("\n", "");
  }

  private static Boolean isEmpty(String value) {
    return value == null || value.trim().equalsIgnoreCase("");
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
  static class RecordDTO {
    Integer index;
    Map<String, String> record;
    List<InvalidField> fieldErrors;

    public void addFieldError(String fieldName, InvalidField.Reason reason, Integer index) {
      fieldErrors.add(new InvalidField(fieldName, reason, index));
    }

    public Boolean hasFieldErrors() {
      return fieldErrors.size() > 0;
    }
  }
}
