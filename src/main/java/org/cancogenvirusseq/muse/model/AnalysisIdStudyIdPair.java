package org.cancogenvirusseq.muse.model;

import java.util.UUID;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AnalysisIdStudyIdPair {
  @NonNull UUID analysisId;
  @NonNull String studyId;
}
