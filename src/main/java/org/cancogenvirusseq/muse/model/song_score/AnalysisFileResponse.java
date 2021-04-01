package org.cancogenvirusseq.muse.model.song_score;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AnalysisFileResponse {
  String objectId;
  String fileName;
  String fileSize;
  String fileMd5sum;
}
