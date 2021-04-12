package org.cancogenvirusseq.muse.exceptions.songScoreClient;

import java.util.Map;
import org.cancogenvirusseq.muse.exceptions.MuseBaseException;
import org.cancogenvirusseq.muse.model.song_score.ClientInputError;
import org.springframework.http.HttpStatus;

public class BadRequestException extends Throwable implements MuseBaseException {
  Map<String, Object> errorInfo;
  String msg;

  public BadRequestException(ClientInputError badClientInputError) {
    this.errorInfo = badClientInputError.getErrorInfo();
    this.msg = badClientInputError.getMsg();
  }

  @Override
  public HttpStatus getStatusCode() {
    return HttpStatus.BAD_REQUEST;
  }

  @Override
  public String getErrorMessage() {
    return msg;
  }

  @Override
  public Map<String, Object> getErrorInfo() {
    return errorInfo;
  }
}
