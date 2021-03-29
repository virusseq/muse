package org.cancogenvirusseq.seqdata.components;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.StringWriter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.cancogenvirusseq.seqdata.model.TsvParserProperties;
import reactor.core.publisher.Flux;

@Slf4j
@RequiredArgsConstructor
public class TsvParser implements Function<String, Flux<ObjectNode>> {
  final TsvParserProperties tsvParserProperties;

  public Flux<ObjectNode> apply(String s) {
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

  public static Flux<Map<String, String>> parseTsvStrToFlatRecords(String s) {
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

    return Flux.fromStream(rows);
  }

  private String convertRecordToPayload(Map<String, String> valuesMap) {
    val context = new VelocityContext();
    valuesMap.forEach(context::put);
    val writer = new StringWriter();
    Velocity.evaluate(context, writer, "", tsvParserProperties.getPayloadJsonTemplate());
    return writer.toString();
  }
}
