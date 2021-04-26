/*
 * Copyright (c) 2021 The Ontario Institute for Cancer Research. All rights reserved
 *
 * This program and the accompanying materials are made available under the terms of the GNU Affero General Public License v3.0.
 * You should have received a copy of the GNU Affero General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.cancogenvirusseq.muse.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.cancogenvirusseq.muse.components.SongScoreClient;
import org.cancogenvirusseq.muse.exceptions.MuseBaseException;
import org.cancogenvirusseq.muse.exceptions.download.DownloadInfoFetchException;
import org.cancogenvirusseq.muse.exceptions.download.UnknownException;
import org.cancogenvirusseq.muse.model.DownloadInfoFetchResult;
import org.cancogenvirusseq.muse.model.song_score.SongScoreServerException;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class DownloadsService {

  final SongScoreClient songScoreClient;

  public Flux<DataBuffer> download(List<UUID> objectIds) {
    return Flux.fromIterable(objectIds)
        .flatMap(this::fetchDownloadInfoFromSong)
        .collectList()
        .flatMap(
            results -> {
              // see if any results have error
              if (results.stream().anyMatch(this::isFetchResultNotOk)) {
                return Mono.error(new DownloadInfoFetchException(results));
              }
              // return if no errors found here
              return Mono.just(results);
            })
        .flatMapMany(Flux::fromIterable)
        .map(DownloadInfoFetchResult::getFileInfo)
        // file will exist because we already checked for valid analysis
        .map(Optional::get)
        .concatMap(
            analysisFileResponse -> {
              val objectId = analysisFileResponse.getObjectId();
              return Flux.concat(songScoreClient.downloadObject(objectId), newLineBuffer());
            })
        .onErrorMap(t -> !(t instanceof MuseBaseException), t -> new UnknownException());
  }

  private static Flux<DataBuffer> newLineBuffer() {
    val buffer = new DefaultDataBufferFactory().allocateBuffer(4);
    val newLIne = "\r\n";
    buffer.write(newLIne.getBytes());
    return Flux.just(buffer);
  }

  private Mono<DownloadInfoFetchResult> fetchDownloadInfoFromSong(UUID objectIds) {
    return songScoreClient
        .getFileEntityFromSong(objectIds)
        .flatMap(
            legacyFileEntity ->
                songScoreClient.getAnalysis(
                    legacyFileEntity.getStudyId(), legacyFileEntity.getAnalysisId()))
        .map(analysis -> new DownloadInfoFetchResult(objectIds, analysis))
        .onErrorResume(
            SongScoreServerException.class,
            t -> Mono.just(new DownloadInfoFetchResult(objectIds, t)));
  }

  // Result isNotOk if analysis is missing, is not published or has no file objects
  private Boolean isFetchResultNotOk(DownloadInfoFetchResult fetchResult) {
    return fetchResult.getException().isPresent()
        || fetchResult
            .getAnalysis()
            .map(a -> !a.isPublished() || !a.hasFiles())
            // analysis doesn't exist so return true
            .orElse(true);
  }
}
