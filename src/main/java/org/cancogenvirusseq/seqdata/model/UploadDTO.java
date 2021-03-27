package org.cancogenvirusseq.seqdata.model;

import lombok.*;

import java.util.UUID;

@Data
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class UploadDTO {
  @NonNull UUID userId;
  @NonNull private UUID submitSetId;
  @NonNull private String studyId;
  @NonNull private String submitterSampleId;
  @NonNull private UploadStatus status;
  private UUID analysisId;
  private String error;
}
