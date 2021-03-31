package org.cancogenvirusseq.muse.components.songscore.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AnalysisFileResponse {
    String objectId;
    String fileName;
    String fileSize;
    String fileMd5sum;
}
