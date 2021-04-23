package org.cancogenvirusseq.muse.model.tsv_parser;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@NoArgsConstructor
@RequiredArgsConstructor
public class InvalidField {
  @NonNull String fieldName;
  @NonNull Reason reason;
  @NonNull Integer index;

  public enum Reason {
    EXPECTING_NUMBER_TYPE,
    NOT_ALLOWED_TO_BE_EMPTY
  }
}
