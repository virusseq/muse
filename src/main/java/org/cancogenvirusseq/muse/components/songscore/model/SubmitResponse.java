package org.cancogenvirusseq.muse.components.songscore.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SubmitResponse {
    String analysisId;
    String status;
}
