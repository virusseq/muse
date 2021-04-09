package org.cancogenvirusseq.muse.exceptions.submission;

import java.util.List;
import java.util.Map;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.cancogenvirusseq.muse.exceptions.MuseBaseException;

@Value
@EqualsAndHashCode(callSuper = true)
public class SubmissionFilesException extends Throwable implements MuseBaseException {
  List<String> filesSubmitted;

  @Override
  public String getErrorMessage() {
    return "Submission must contain exactly one .tsv file and one or more .fasta files";
  }

  @Override
  public Map<String, Object> getErrorInfo() {
    return Map.of("filesSubmitted", filesSubmitted);
  }
}
