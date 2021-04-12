package org.cancogenvirusseq.muse.model.song_score;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.springframework.http.HttpStatus;

import static java.lang.String.format;

@Value
@EqualsAndHashCode(callSuper = true)
public class SongScoreClientException extends Throwable {
  HttpStatus status;
  String msg;

  public String toString() {
    return format("%s - %s", status.toString(), msg);
  }
}
