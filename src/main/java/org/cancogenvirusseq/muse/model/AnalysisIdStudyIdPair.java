package org.cancogenvirusseq.muse.model;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AnalysisIdStudyIdPair {
  @NonNull UUID analysisId;
  @NonNull String studyId;
}
