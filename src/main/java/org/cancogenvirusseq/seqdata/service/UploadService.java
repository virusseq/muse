package org.cancogenvirusseq.seqdata.service;

import org.cancogenvirusseq.seqdata.api.model.UploadListResponse;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class UploadService {
  public Mono<UploadListResponse> getUploads(String userId, Integer pageSize, Integer pageToken) {
    return Mono.just(new UploadListResponse());
  }

  public Mono<UploadListResponse> getUploadsForSubmitSetId(
      String submitSetId, String userId, Integer pageSize, Integer pageToken) {
    return Mono.just(new UploadListResponse());
  }
}
