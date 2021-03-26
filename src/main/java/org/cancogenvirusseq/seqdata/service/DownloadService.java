package org.cancogenvirusseq.seqdata.service;

import org.cancogenvirusseq.seqdata.api.model.DownloadRequest;
import org.cancogenvirusseq.seqdata.api.model.DownloadResponse;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class DownloadService {
  public Mono<DownloadResponse> download(DownloadRequest downloadRequest) {
    return Mono.just(new DownloadResponse());
  }
}
