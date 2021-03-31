package org.cancogenvirusseq.muse.components;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.cancogenvirusseq.muse.model.TsvParserProperties;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

@Slf4j
@RequiredArgsConstructor
public class TsvParser implements Function<String, Stream<ObjectNode>> {
  final TsvParserProperties tsvParserProperties;

  public Stream<ObjectNode> apply(String s) {
    return parseTsvStrToFlatRecords(s)
        .map(this::convertRecordToPayload)
        .map(
            jsonStr -> {
              try {
                return new ObjectMapper().readValue(jsonStr, ObjectNode.class);
              } catch (JsonProcessingException e) {
                e.printStackTrace();
                return null;
              }
            })
        .filter(Objects::nonNull);
  }

  public static Stream<Map<String, String>> parseTsvStrToFlatRecords(String s) {
    val lines = s.split("\n");
    val headers = lines[0].trim().split("\t");

    Stream<Map<String, String>> rows =
        Arrays.stream(lines)
            .skip(1)
            .filter(line -> line != null && !line.trim().equals(""))
            .map(
                line -> {
                  val data = line.split("\t");
                  val dataJson = new HashMap<String, String>();

                  for (int i = 0; i < headers.length; ++i) {
                    dataJson.put(headers[i], i > data.length ? "" : data[i]);
                  }
                  return dataJson;
                });

    return rows;
  }

  private String convertRecordToPayload(Map<String, String> valuesMap) {
    val context = new VelocityContext();
    valuesMap.forEach(context::put);
    val writer = new StringWriter();
    Velocity.evaluate(context, writer, "", tsvParserProperties.getPayloadJsonTemplate());
    return writer.toString();
  }
}
