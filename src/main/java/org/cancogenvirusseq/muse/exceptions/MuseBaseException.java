package org.cancogenvirusseq.muse.exceptions;

import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

public interface MuseBaseException {
  default HttpStatus getStatusCode() {
    return HttpStatus.BAD_REQUEST;
  }

  String getErrorMessage();

  default Map<String, Object> getErrorInfo() {
    return new HashMap<>();
  }
}
