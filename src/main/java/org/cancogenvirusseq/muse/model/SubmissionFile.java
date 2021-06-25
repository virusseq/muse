package org.cancogenvirusseq.muse.model;

import lombok.Builder;
import lombok.Value;

/** The object type that is ultimately uploaded to SCORE */
@Value
@Builder(toBuilder = true)
public class SubmissionFile {
  String fileExtension;
  Integer fileSize;
  String fileMd5sum;
  String content;
  String dataType;
  String fileType;
  String fileAccess = "open";
  String submittedFileName;
}
