package org.cancogenvirusseq.seqdata.model;

import com.google.common.collect.ImmutableList;
import lombok.Value;

@Value
public class TsvParserProperties {
  ImmutableList<TsvFieldSchema> fieldSchemas;
  String payloadJsonTemplate;
}
