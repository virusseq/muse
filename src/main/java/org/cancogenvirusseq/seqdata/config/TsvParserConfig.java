package org.cancogenvirusseq.seqdata.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import java.io.*;
import java.net.URL;
import java.util.List;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.cancogenvirusseq.seqdata.model.TsvFieldSchema;
import org.cancogenvirusseq.seqdata.model.TsvParserProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class TsvParserConfig {

  @Bean
  @SneakyThrows
  public TsvParserProperties tsvParserProperties() {
    URL url1 = Resources.getResource("tsv-schema.json");
    val mapper = new ObjectMapper();
    val fieldSchemas = mapper.readValue(url1, new TypeReference<List<TsvFieldSchema>>() {});

    log.debug("Loaded TSV schema - {}", fieldSchemas);

    URL url2 = Resources.getResource("payload-template.vm");
    val template = asString(url2);
    return new TsvParserProperties(ImmutableList.copyOf(fieldSchemas), template);
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
