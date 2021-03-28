package org.cancogenvirusseq.seqdata.model;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
public class SubmissionDTO {
    @NonNull UUID submissionId;
    @NonNull UUID userId;
    @NonNull LocalDateTime createdAt; // todo: do we want OffsetDateTime?
    @NonNull List<String> originalFileNames;
}
