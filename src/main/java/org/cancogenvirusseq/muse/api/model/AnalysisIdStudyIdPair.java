package org.cancogenvirusseq.muse.api.model;

import java.util.UUID;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AnalysisIdStudyIdPair {
  @NonNull UUID analysisId;
  @NonNull String studyId;
}
