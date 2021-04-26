package org.cancogenvirusseq.muse.exceptions.submission;

import java.util.List;
import java.util.Map;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.cancogenvirusseq.muse.exceptions.MuseBaseException;

@Value
@EqualsAndHashCode(callSuper = true)
public class FoundInvalidFilesException extends Throwable implements MuseBaseException {
  List<String> isolateWithEmptyData;

  @Override
  public String getMessage() {
    return "Found samples with no data (only headers)!";
  }

  public Map<String, Object> getErrorInfo() {
    return Map.of("isolateWithEmptyData", isolateWithEmptyData);
  }
}
