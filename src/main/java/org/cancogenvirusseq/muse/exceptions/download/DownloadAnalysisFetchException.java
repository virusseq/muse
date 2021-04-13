package org.cancogenvirusseq.muse.exceptions.download;

import org.cancogenvirusseq.muse.exceptions.MuseBaseException;
import org.cancogenvirusseq.muse.model.DownloadFetchResult;
import org.cancogenvirusseq.muse.model.song_score.SongScoreClientException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DownloadAnalysisFetchException extends Throwable implements MuseBaseException {
  List<Map<String, String>> analysisWithErrors;
  Integer statusCode;

  public DownloadAnalysisFetchException(List<DownloadFetchResult> analysisErrors) {

    this.analysisWithErrors = analysisErrors.stream()
                                      .filter(DownloadFetchResult::hasException)
                                      .map(ae -> {
      // Map 404 and 400 exceptions since those are user errors
      if (is404Or400(ae.getException().getStatus())) {
        return Map.of("analysisId", ae.getAnalysisId().toString(), "errorMsg", ae.getException().getMsg());
      // all other exceptions are internal between muse and SONG/score
      } else {
        return Map.of("analysisId", ae.getAnalysisId().toString(), "errorMsg", "Internal Server Error!");
      }
    }).collect(Collectors.toUnmodifiableList());
    this.statusCode = analysisErrors.stream().mapToInt(ae -> ae.getException().getStatus().value()).max().orElse(418);
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

  private static Boolean is404Or400(HttpStatus status) {
    return status.equals(HttpStatus.BAD_REQUEST) || status.equals(HttpStatus.NOT_FOUND);
  }
}
