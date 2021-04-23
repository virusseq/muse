package org.cancogenvirusseq.muse.model.song_score;

import static java.lang.String.format;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.springframework.http.HttpStatus;

@Value
@EqualsAndHashCode(callSuper = true)
public class SongScoreServerException extends Throwable {
  HttpStatus status;
  String message;

  public String toString() {
    return format("%s - %s", status.toString(), message);
  }
}
