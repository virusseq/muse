package org.cancogenvirusseq.muse.exceptions.download;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.cancogenvirusseq.muse.exceptions.MuseBaseException;

@Value
@EqualsAndHashCode(callSuper = true)
public class DownloadException extends Throwable implements MuseBaseException {
  @Override
  public String getErrorMessage() {
    return "Internal Server Error, please try again later";
  }
}
