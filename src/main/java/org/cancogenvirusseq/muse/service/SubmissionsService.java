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

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.cancogenvirusseq.muse.api.model.SubmissionCreateResponse;
import org.cancogenvirusseq.muse.api.model.SubmissionListResponse;
import org.cancogenvirusseq.muse.model.SubmissionEvent;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingByConcurrent;

@Service
@Slf4j
public class SubmissionsService {
  public Sinks.Many<SubmissionEvent> submissionsSink =
      Sinks.many().unicast().onBackpressureBuffer();

  public Mono<SubmissionListResponse> getSubmissions(
      String userId, Integer pageSize, Integer pageToken) {
    return Mono.just(new SubmissionListResponse(Collections.emptyList()));
  }

  public Mono<SubmissionCreateResponse> submit(Flux<FilePart> fileParts) {
    // generate a submissionId
    val submissionId = UUID.randomUUID();

    // parse the .tsv into records, on error throw, write as jsonp to
    // "tmp/${submissionId}/records.jsonp"

    // process the fasta files, write sequence files to tmp dir and also write fastaFileMeta to
    // "tmp/${submissionId}/fastaFileMeta.json"

    // emit the SubmissionEvent to sink

    // send response

    return fileParts
        .transform(this::validateSubmission)
        .flatMap(
            filePart ->
                filePart
                    .content()
                    .map(
                        dataBuffer -> {
                          val bytes = new byte[dataBuffer.readableByteCount()];
                          dataBuffer.read(bytes);
                          DataBufferUtils.release(dataBuffer);

                          return new String(bytes, StandardCharsets.UTF_8);
                        }))
        .collect(Collectors.toList())
        .map(fileContents -> new SubmissionCreateResponse(submissionId.toString()));
  }

  private Flux<FilePart> validateSubmission(Flux<FilePart> fileParts) {
    return fileParts
        .collect(
            groupingByConcurrent(
                part ->
                    Optional.of(part.filename())
                        .filter(f -> f.contains("."))
                        .map(f -> f.substring(part.filename().lastIndexOf(".") + 1))
                        .orElse("invalid")))
        .handle(
            (fileTypeMap, sink) -> {
              // validate that we have exactly one .tsv and one or more fasta files
              if (fileTypeMap.getOrDefault("tsv", Collections.emptyList()).size() == 1
                  && fileTypeMap.getOrDefault("fasta", Collections.emptyList()).size() >= 1) {
                sink.next(true);
              } else {
                sink.error(
                    new IllegalArgumentException(
                        "Submission must contain exactly one .tsv file and one or more .fasta files"));
              }
            })
        .thenMany(fileParts);
  }
}
