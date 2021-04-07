package org.cancogenvirusseq.muse.repository.model;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

@WritingConverter
public class UploadStatusConverter implements Converter<UploadStatus, UploadStatus> {
  @Override
  public UploadStatus convert(UploadStatus source) {
    return source;
  }
}
