package org.cancogenvirusseq.muse.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AnalysisIdStudyIdPair {
  @NonNull UUID analysisId;
  @NonNull String studyId;
}
