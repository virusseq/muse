package org.cancogenvirusseq.muse.model.song_score;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SubmitResponse {
  String analysisId;
  String status;
}
