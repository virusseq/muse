package org.cancogenvirusseq.muse.model.song_score_client;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SubmitResponse {
  String analysisId;
  String status;
}
