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

package org.cancogenvirusseq.seqdata.service;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.cancogenvirusseq.seqdata.api.model.SubmissionCreateResponse;
import org.cancogenvirusseq.seqdata.api.model.SubmissionListResponse;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class SubmissionsService {
  public Mono<SubmissionListResponse> getSubmissions(
      String userId, Integer pageSize, Integer pageToken) {
    return Mono.just(new SubmissionListResponse(Collections.emptyList()));
  }

  public Mono<SubmissionCreateResponse> submit(Flux<FilePart> fileParts) {
    return fileParts
        .filter(
            filePart ->
                filePart.filename().endsWith(".fasta") || filePart.filename().endsWith(".tsv"))
        .doOnDiscard(
            FilePart.class,
            discarded ->
                log.info("The file: {}, must be of type '.tsv' or '.fasta'", discarded.filename()))
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
        .map(
            fileContents ->
                new SubmissionCreateResponse(
                    fileContents.isEmpty() ? "no file processed" : fileContents.get(0)));
  }
}
