package org.cancogenvirusseq.seqdata.api.model;

import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import org.cancogenvirusseq.seqdata.model.SubmissionDTO;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@ApiModel(description = "A list of submissions")
public class SubmissionListResponse {
  @NonNull List<SubmissionDTO> submissions;
}
