package org.cancogenvirusseq.muse.exceptions.submission;

import java.util.List;
import java.util.Map;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.cancogenvirusseq.muse.exceptions.MuseBaseException;

@Value
@EqualsAndHashCode(callSuper = true)
public class MissingDataException extends Throwable implements MuseBaseException {
  List<String> fastaHeaderInFileMissingInTsv;
  List<String> fastaHeaderInRecordMissingInFile;

  @Override
  public String getMessage() {
    return "Found records that are missing samples and/or samples that are missing records";
  }

  public Map<String, Object> getErrorInfo() {
    return Map.of(
        "fastaHeaderInFileMissingInTsv", fastaHeaderInFileMissingInTsv,
        "fastaHeaderInRecordMissingInFile", fastaHeaderInRecordMissingInFile);
  }
}
