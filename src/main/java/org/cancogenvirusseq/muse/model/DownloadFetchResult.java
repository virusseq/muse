package org.cancogenvirusseq.muse.model;

import lombok.Getter;
import org.cancogenvirusseq.muse.model.song_score.AnalysisFileResponse;
import org.cancogenvirusseq.muse.model.song_score.SongScoreClientException;

import java.util.UUID;

@Getter
public class DownloadFetchResult {
    final UUID analysisId;
    final AnalysisFileResponse fileResponse;
    final SongScoreClientException exception;

    public DownloadFetchResult(UUID analysisId, AnalysisFileResponse fileResponse) {
        this.analysisId = analysisId;
        this.fileResponse = fileResponse;
        this.exception = null;
    }

    public DownloadFetchResult(UUID analysisId, SongScoreClientException exception) {
        this.analysisId = analysisId;
        this.fileResponse = null;
        this.exception = exception;
    }

    public Boolean hasFileResponse() {
        return fileResponse != null;
    }
}