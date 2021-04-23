package org.cancogenvirusseq.muse.model.tsv_parser;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TsvFieldSchema {
  String name;
  ValueType valueType;
  Boolean requireNotEmpty = false;

  public enum ValueType {
    string,
    number
  }
}
