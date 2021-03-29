package org.cancogenvirusseq.seqdata.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TsvFieldSchema {
  String name;
  String valueType;
}
