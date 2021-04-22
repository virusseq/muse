package org.cancogenvirusseq.muse.model.song_score;

import java.util.UUID;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@NoArgsConstructor
public class LegacyFileEntity {
  String id;
  String projectCode;
  UUID gnosId;

  public LegacyFileEntity(String objectId, String studyId, UUID analysisId) {
    this.id = objectId;
    this.projectCode = studyId;
    this.gnosId = analysisId;
  }

  public String getObjectId() {
    return id;
  }

  public String getStudyId() {
    return projectCode;
  }

  public UUID getAnalysisId() {
    return gnosId;
  }
}
