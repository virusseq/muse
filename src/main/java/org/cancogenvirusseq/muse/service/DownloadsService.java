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

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.cancogenvirusseq.muse.api.model.DownloadRequest;
import org.cancogenvirusseq.muse.components.SongScoreClient;
import org.cancogenvirusseq.muse.exceptions.MuseBaseException;
import org.cancogenvirusseq.muse.exceptions.DownloadException;
import org.cancogenvirusseq.muse.exceptions.songScoreClient.UnknownException;
import org.cancogenvirusseq.muse.model.DownloadFetchResult;
import org.cancogenvirusseq.muse.model.song_score.AnalysisFileResponse;
import org.cancogenvirusseq.muse.model.song_score.SongScoreClientException;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toUnmodifiableList;

@Slf4j
@Service
@RequiredArgsConstructor
public class DownloadsService {

  final SongScoreClient songScoreClient;

  public Flux<DataBuffer> download(DownloadRequest downloadRequest) {
    return Flux.fromIterable(downloadRequest.getAnalysisIds())
       .flatMap(id -> songScoreClient
                              .getFileSpecFromSong(downloadRequest.getStudyId(), id)
                              .map(fileSpec -> new DownloadFetchResult(id, fileSpec))
                              .onErrorResume(SongScoreClientException.class, t -> Mono.just(new DownloadFetchResult(id, t))))
       .collectList()
       .map(analysisInfos -> analysisInfos.stream().collect(groupingBy(DownloadFetchResult::hasFileResponse)))
       .flatMap(mappedInfos -> {
           if (mappedInfos.get(false).size() == 0) {
               return Mono.just(mappedInfos.get(true));
           }
           return Mono.error(new DownloadException(
                   mappedInfos.get(false).stream()
                           .map(DownloadFetchResult::getException)
                           .collect(toUnmodifiableList())));
       })
        .flatMapMany(Flux::fromIterable)
        .map(DownloadFetchResult::getFileResponse)
        .flatMap(
            analysisFileResponse -> {
              val objectId = analysisFileResponse.getObjectId();
              return songScoreClient.downloadObject(objectId);
            })
        .onErrorMap(t -> !(t instanceof MuseBaseException), t -> new UnknownException());
  }
}
