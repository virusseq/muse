package org.cancogenvirusseq.muse.exceptions;

import java.util.Map;

import lombok.AllArgsConstructor;
import org.cancogenvirusseq.muse.exceptions.MuseBaseException;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
public class GenericException extends Throwable implements MuseBaseException {
  String msg;
  Map<String, Object> errorInfo;

  @Override
  public HttpStatus getStatusCode() {
    return HttpStatus.I_AM_A_TEAPOT;
  }

  @Override
  public String getErrorMessage() {
    return "Failed to download, with given analysisIds";
  }

  @Override
  public Map<String, Object> getErrorInfo() {
    return errorInfo;
  }
}
