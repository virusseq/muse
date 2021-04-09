package org.cancogenvirusseq.muse.exceptions.submission;

import java.util.List;
import java.util.Map;
import lombok.Value;
import org.cancogenvirusseq.muse.exceptions.MuseBaseException;

@Value
public class MissingHeadersException extends Throwable implements MuseBaseException {
  List<String> missingHeaders;
  List<String> unknownHeaders;

  @Override
  public String getErrorMessage() {
    return "Headers are incorrect!";
  }

  public Map<String, Object> getErrorInfo() {
    return Map.of(
        "missingHeaders", missingHeaders,
        "unknownHeaders", unknownHeaders);
  }
}
