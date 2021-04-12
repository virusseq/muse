package org.cancogenvirusseq.muse.exceptions;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import org.cancogenvirusseq.muse.model.song_score.SongScoreClientException;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
public class DownloadException extends Throwable implements MuseBaseException {
  List<SongScoreClientException> analysisErrors;

  @Override
  public HttpStatus getStatusCode() {
    return HttpStatus.I_AM_A_TEAPOT;
  }

  @Override
  public String getErrorMessage() {
    return "Failed to download, with given analysisIds";
  }

  @Override
  public Map<String, Object> getErrorInfo() {
    return Map.of("analysisWithErrors", analysisErrors);
  }
}
