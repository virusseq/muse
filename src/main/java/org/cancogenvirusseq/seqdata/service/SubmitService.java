package org.cancogenvirusseq.seqdata.service;

import org.cancogenvirusseq.seqdata.api.model.SubmitRequest;
import org.cancogenvirusseq.seqdata.api.model.SubmitResponse;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class SubmitService {
  public Mono<SubmitResponse> submit(SubmitRequest submitRequest) {
    return Mono.just(new SubmitResponse());
  }
}
