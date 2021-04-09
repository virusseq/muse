package org.cancogenvirusseq.muse.exceptions.submission;

import java.util.List;
import java.util.Map;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.cancogenvirusseq.muse.exceptions.MuseBaseException;
import org.cancogenvirusseq.muse.model.tsv_parser.InvalidField;

@Value
@EqualsAndHashCode(callSuper = true)
public class InvalidFieldsException extends Throwable implements MuseBaseException {
  String msg = "Found records with invalid fields";
  List<InvalidField> invalidFields;

  @Override
  public Map<String, Object> getErrorObject() {
    return Map.of(
        "message", msg,
        "invalidFields", invalidFields);
  }
}
