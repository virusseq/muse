package org.cancogenvirusseq.muse.exceptions.submission;

import java.util.List;
import java.util.Map;
import lombok.Value;
import org.cancogenvirusseq.muse.exceptions.MuseBaseException;

@Value
public class MissingHeadersException extends Throwable implements MuseBaseException {
  String msg = "Headers are incorrect!";
  List<String> missingHeaders;
  List<String> unknownHeaders;

  public Map<String, Object> getErrorObject() {
    return Map.of(
        "message", msg,
        "missingHeaders", missingHeaders,
        "unknownHeaders", unknownHeaders);
  }
}
