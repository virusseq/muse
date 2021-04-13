package org.cancogenvirusseq.muse.exceptions.download;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.cancogenvirusseq.muse.exceptions.MuseBaseException;
import org.cancogenvirusseq.muse.model.DownloadAnalysisFetchResult;
import org.springframework.http.HttpStatus;

public class DownloadAnalysisFetchException extends Throwable implements MuseBaseException {
  List<Map<String, String>> analysisInfo;
  Integer statusCode;

  public DownloadAnalysisFetchException(List<DownloadAnalysisFetchResult> analysisErrors) {

    this.analysisInfo =
        analysisErrors.stream()
            .map(
                fetchResult ->
                    Map.of(
                        "analysisId",
                        fetchResult.getAnalysisId().toString(),
                        "msg",
                        fetchResult.getResultMsg()))
            .collect(Collectors.toUnmodifiableList());

    // We take the max status code of any songscore exception or else tea pot
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
    return Map.of("analysisFetchResult", analysisInfo);
  }
}
