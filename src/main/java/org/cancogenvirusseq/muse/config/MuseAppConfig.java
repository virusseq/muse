package org.cancogenvirusseq.muse.config;

import static java.util.stream.Collectors.toUnmodifiableList;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import java.io.*;
import java.net.URL;
import java.util.List;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.cancogenvirusseq.muse.model.tsv_parser.TsvFieldSchema;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Getter
@Configuration
public class MuseAppConfig {
  ImmutableList<String> expectedTsvHeaders;
  ImmutableList<TsvFieldSchema> tsvFieldSchemas;
  String payloadJsonTemplate;

  @SneakyThrows
  public MuseAppConfig() {
    URL url1 = Resources.getResource("tsv-schema.json");
    val mapper = new ObjectMapper();
    val fieldSchemas = mapper.readValue(url1, new TypeReference<List<TsvFieldSchema>>() {});

    URL url2 = Resources.getResource("payload-template");

    this.tsvFieldSchemas = ImmutableList.copyOf(fieldSchemas);
    this.expectedTsvHeaders =
        ImmutableList.copyOf(
            fieldSchemas.stream().map(TsvFieldSchema::getName).collect(toUnmodifiableList()));
    this.payloadJsonTemplate = asString(url2);
  }

  @SneakyThrows
  private static String asString(URL oracle) {
    val sb = new StringBuilder();
    val in = new BufferedReader(new InputStreamReader(oracle.openStream()));

    String inputLine;
    while ((inputLine = in.readLine()) != null) sb.append(inputLine);
    in.close();

    return sb.toString();
  }
}
