package org.cancogenvirusseq.muse.model.song_score;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Analysis {
  private static final String PUBLISED_STATE = "PUBLISHED";

  String analysisId;
  String studyId;
  String analysisState;
  List<AnalysisFile> files;

  public Boolean isPublished() {
    return analysisState.equalsIgnoreCase(PUBLISED_STATE);
  }

  public Boolean hasFiles() {
    return files.size() > 0;
  }
}
