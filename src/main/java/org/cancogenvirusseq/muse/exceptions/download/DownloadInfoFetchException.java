package org.cancogenvirusseq.muse.exceptions.download;

import org.cancogenvirusseq.muse.exceptions.MuseBaseException;
import org.cancogenvirusseq.muse.model.DownloadInfoFetchResult;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DownloadInfoFetchException extends Throwable implements MuseBaseException {
  List<Map<String, String>> analysisInfo;
  Integer statusCode;

  public DownloadInfoFetchException(List<DownloadInfoFetchResult> analysisErrors) {

    this.analysisInfo =
        analysisErrors.stream()
            .map(
                fetchResult ->
                    Map.of(
                        "analysisId",
                        fetchResult.getAnalysisId(),
                        "objectId",
                        fetchResult.getObjectId().toString(),
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
  public String getMessage() {
    return "Failed to download, with given analysisIds";
  }

  @Override
  public Map<String, Object> getErrorInfo() {
    return Map.of("analysisFetchResult", analysisInfo);
  }
}
