package org.cancogenvirusseq.muse.exceptions.submission;

import java.util.List;
import java.util.Map;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.cancogenvirusseq.muse.exceptions.MuseBaseException;

@Value
@EqualsAndHashCode(callSuper = true)
public class InvalidHeadersException extends Throwable implements MuseBaseException {
  List<String> missingHeaders;
  List<String> unknownHeaders;

  @Override
  public String getMessage() {
    return "Headers are incorrect!";
  }

  public Map<String, Object> getErrorInfo() {
    return Map.of(
        "missingHeaders", missingHeaders,
        "unknownHeaders", unknownHeaders);
  }
}
