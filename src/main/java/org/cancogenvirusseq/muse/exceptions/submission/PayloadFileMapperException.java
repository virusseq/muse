package org.cancogenvirusseq.muse.exceptions.submission;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.cancogenvirusseq.muse.exceptions.MuseBaseException;

import java.util.List;
import java.util.Map;

@Value
@EqualsAndHashCode(callSuper = true)
public class PayloadFileMapperException extends Throwable implements MuseBaseException {
  List<String> isolateInFileMissingInTsv;
  List<String> isolateInRecordMissingInFile;

  @Override
  public String getMessage() {
    return "Found records that are missing samples and/or samples that are missing records";
  }

  public Map<String, Object> getErrorInfo() {
    return Map.of(
        "isolateInFileMissingInTsv", isolateInFileMissingInTsv,
        "isolateInRecordMissingInFile", isolateInRecordMissingInFile);
  }
}
