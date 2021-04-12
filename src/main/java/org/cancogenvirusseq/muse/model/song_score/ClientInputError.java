package org.cancogenvirusseq.muse.model.song_score;

import java.util.Map;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = true)
public class ClientInputError extends Throwable {
  String msg;
  Map<String, Object> errorInfo;
}
