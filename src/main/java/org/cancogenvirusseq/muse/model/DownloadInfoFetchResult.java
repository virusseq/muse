package org.cancogenvirusseq.muse.model;

import bio.overture.aria.exceptions.ClientException;
import bio.overture.aria.model.Analysis;
import bio.overture.aria.model.AnalysisFile;
import java.util.Optional;
import java.util.UUID;
import lombok.Getter;
import org.springframework.http.HttpStatus;

public class DownloadInfoFetchResult {
  @Getter final UUID objectId;
  final Analysis analysis;
  final ClientException exception;

  public DownloadInfoFetchResult(UUID objectId, Analysis analysis) {
    this.objectId = objectId;
    this.analysis = analysis;
    this.exception = null;
  }

  public DownloadInfoFetchResult(UUID objectId, ClientException exception) {
    this.objectId = objectId;
    this.analysis = null;
    this.exception = exception;
  }

  public String getResultMsg() {
    if (isExceptionStatus404Or400()) {
      return Optional.ofNullable(exception)
          .map(ClientException::getMessage)
          .orElse(
              "Detailed error information unavailable, please consult server logs or contact support");
    }
    if (analysis == null) {
      return "Analysis was not found";
    }
    if (!analysis.isPublished()) {
      return "Analysis is not published";
    }
    if (!analysis.hasFiles()) {
      return "Analysis doesn't have files";
    }
    return "Analysis is OK";
  }

  public Optional<ClientException> getException() {
    return Optional.ofNullable(exception);
  }

  public Optional<Analysis> getAnalysis() {
    return Optional.ofNullable(analysis);
  }

  public String getAnalysisId() {
    return Optional.ofNullable(analysis).map(Analysis::getAnalysisId).orElse("NO ANALYSIS ID");
  }

  public Optional<AnalysisFile> getFileInfo() {
    return getAnalysis().map(a -> a.getFiles().get(0));
  }

  private Boolean isExceptionStatus404Or400() {
    return getException()
        .map(ClientException::getStatus)
        .map(status -> status.equals(HttpStatus.BAD_REQUEST) || status.equals(HttpStatus.NOT_FOUND))
        .orElse(false);
  }
}
