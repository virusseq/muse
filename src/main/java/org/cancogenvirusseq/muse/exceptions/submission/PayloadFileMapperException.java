package org.cancogenvirusseq.muse.exceptions.submission;

import java.util.List;
import java.util.Map;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.cancogenvirusseq.muse.exceptions.MuseBaseException;

@Value
@EqualsAndHashCode(callSuper = true)
public class PayloadFileMapperException extends Throwable implements MuseBaseException {
  String msg = "Found records that are missing samples and/or samples that are missisng records";
  List<String> sampleIdInFileMissingInTsv;
  List<String> sampleIdInRecordMissingInFile;

  @Override
  public Map<String, Object> getErrorObject() {
    return Map.of(
        "message", msg,
        "sampleIdInFileMissingInTsv", sampleIdInFileMissingInTsv,
        "sampleIdInRecordMissingInFile", sampleIdInRecordMissingInFile);
  }
}
