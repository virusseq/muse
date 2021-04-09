package org.cancogenvirusseq.muse.exceptions;

import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;

public interface MuseBaseException {
  default HttpStatus getStatusCode() {
    return HttpStatus.BAD_REQUEST;
  }

  String getErrorMessage();

  default Map<String, Object> getErrorInfo() {
    return new HashMap<>();
  }
}
