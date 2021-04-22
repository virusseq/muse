package org.cancogenvirusseq.muse.components;

import static java.util.stream.Collectors.toUnmodifiableList;

import java.util.*;
import java.util.stream.Stream;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.cancogenvirusseq.muse.config.MuseAppConfig;
import org.cancogenvirusseq.muse.exceptions.submission.MissingHeadersException;
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

    return parse(lines, strTsvHeaders.toArray(String[]::new));
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

  private Stream<Map<String, String>> parse(String[] lines, String[] headers) {
    return Arrays.stream(lines)
        .skip(1)
        .parallel()
        .filter(line -> line != null && !line.trim().equals(""))
        .map(
            line -> {
              val data = line.split("\t");

              Map<String, String> record = new HashMap<>();
              for (int i = 0; i < headers.length; ++i) {
                val value = i >= data.length ? "" : data[i];
                record.put(headers[i], cleanup(value));
              }
              return record;
            });
  }

  private static String cleanup(String rawValue) {
    return rawValue.replace("\r", "").replace("\n", "");
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
