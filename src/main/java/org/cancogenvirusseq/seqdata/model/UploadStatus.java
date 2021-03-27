package org.cancogenvirusseq.seqdata.model;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum UploadStatus {
  SUBMITTED,
  PROCESSING,
  ERROR,
  COMPLETE;
}
