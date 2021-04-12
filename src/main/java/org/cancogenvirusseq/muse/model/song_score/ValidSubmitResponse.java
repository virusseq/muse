package org.cancogenvirusseq.muse.model.song_score;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidSubmitResponse {
  String analysisId;
  String status;
}
