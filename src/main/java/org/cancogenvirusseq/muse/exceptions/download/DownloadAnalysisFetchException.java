package org.cancogenvirusseq.muse.exceptions.download;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.cancogenvirusseq.muse.exceptions.MuseBaseException;
import org.cancogenvirusseq.muse.model.DownloadAnalysisFetchResult;
import org.springframework.http.HttpStatus;

public class DownloadAnalysisFetchException extends Throwable implements MuseBaseException {
  List<Map<String, String>> analysisWithErrors;
  Integer statusCode;

  public DownloadAnalysisFetchException(List<DownloadAnalysisFetchResult> analysisErrors) {

    this.analysisWithErrors =
        analysisErrors.stream()
            .map(
                ae -> Map.of("analysisId", ae.getAnalysisId().toString(), "msg", ae.getResultMsg()))
            .collect(Collectors.toUnmodifiableList());
    this.statusCode =
        analysisErrors.stream()
            .filter(fetchResult -> fetchResult.getException().isPresent())
            .mapToInt(fetchResult -> fetchResult.getException().get().getStatus().value())
            .max()
            .orElse(418);
  }

  @Override
  public HttpStatus getStatusCode() {
    return HttpStatus.valueOf(statusCode);
  }

  @Override
  public String getErrorMessage() {
    return "Failed to download, with given analysisIds";
  }

  @Override
  public Map<String, Object> getErrorInfo() {
    return Map.of("analysisWithErrors", analysisWithErrors);
  }
}
