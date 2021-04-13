package org.cancogenvirusseq.muse.model.song_score;

import static java.lang.String.format;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.springframework.http.HttpStatus;

@Value
@EqualsAndHashCode(callSuper = true)
public class SongScoreClientException extends Throwable {
  HttpStatus status;
  String songScoreErrorMsg;

  public String toString() {
    return format("%s - %s", status.toString(), songScoreErrorMsg);
  }
}
