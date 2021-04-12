package org.cancogenvirusseq.muse.model.song_score;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.springframework.http.HttpStatus;

@Value
@EqualsAndHashCode(callSuper = true)
public class SongScoreClientException extends Throwable {
  HttpStatus status;
  String msg;
}
