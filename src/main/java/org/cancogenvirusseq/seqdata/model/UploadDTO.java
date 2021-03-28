package org.cancogenvirusseq.seqdata.model;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class UploadDTO {
  @NonNull UUID userId;
  @NonNull private UUID submissionId;
  @NonNull private String studyId;
  @NonNull private String submitterSampleId;
  @NonNull private UploadStatus status;
  @NonNull List<String> originalFilePair;
  private UUID analysisId;
  private String error;
}
