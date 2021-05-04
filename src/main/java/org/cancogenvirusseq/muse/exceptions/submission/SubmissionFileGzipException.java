package org.cancogenvirusseq.muse.exceptions.submission;

import static java.lang.String.format;

import java.util.Map;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.cancogenvirusseq.muse.exceptions.MuseBaseException;

@Value
@EqualsAndHashCode(callSuper = true)
public class SubmissionFileGzipException extends Throwable implements MuseBaseException {
  String filename;

  @Override
  public String getMessage() {
    return format(
        "Error occurred while attempting to unzip the file '%s', please check that it was compressed correctly",
        filename);
  }

  @Override
  public Map<String, Object> getErrorInfo() {
    return Map.of("filename", filename);
  }
}
