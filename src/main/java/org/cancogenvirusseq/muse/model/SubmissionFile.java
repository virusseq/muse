package org.cancogenvirusseq.muse.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SubmissionFile {
  String fileName;
  Integer fileSize;
  String fileMd5sum;
  String content;
  String dataType;
  String fileType;
  String fileAccess = "OPEN";
  String dataCategory;
}
