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

package org.cancogenvirusseq.muse.api;

import static java.lang.String.format;
import static org.cancogenvirusseq.muse.components.FastaFileProcessor.FASTA_FILE_EXTENSION;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.ByteStreams;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.cancogenvirusseq.muse.api.model.*;
import org.cancogenvirusseq.muse.exceptions.MuseBaseException;
import org.cancogenvirusseq.muse.exceptions.download.UnknownException;
import org.cancogenvirusseq.muse.service.DownloadsService;
import org.cancogenvirusseq.muse.service.SubmissionService;
import org.cancogenvirusseq.muse.service.UploadService;
import org.cancogenvirusseq.muse.utils.SecurityContextWrapper;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ApiController implements ApiDefinition {

  private final SubmissionService submissionService;
  private final UploadService uploadService;
  private final DownloadsService downloadsService;
  private final ObjectMapper objectMapper;

  private static final String CONTENT_DISPOSITION_HEADER = "Content-Disposition";

  public Mono<EntityListResponse<SubmissionDTO>> getSubmissions(
      Integer page, Integer size, Sort.Direction sortDirection, SubmissionSortField sortField) {
    return SecurityContextWrapper.forFlux(submissionService::getSubmissions)
        .apply(PageRequest.of(page, size, Sort.by(sortDirection, sortField.toString())))
        .map(SubmissionDTO::fromDAO)
        .collectList()
        .transform(this::listResponseTransform);
  }

  public Mono<SubmissionCreateResponse> submit(@RequestPart("files") Flux<FilePart> fileParts) {
    return SecurityContextWrapper.forMono(submissionService::submit).apply(fileParts);
  }

  public Mono<EntityListResponse<UploadDTO>> getUploads(
      Integer page,
      Integer size,
      Sort.Direction sortDirection,
      UploadSortField sortField,
      UUID submissionId) {
    return SecurityContextWrapper.forFlux(uploadService::getUploadsPaged)
        .apply(
            PageRequest.of(page, size, Sort.by(sortDirection, sortField.toString())), submissionId)
        .map(UploadDTO::fromDAO)
        .collectList()
        .transform(this::listResponseTransform);
  }

  public Flux<UploadDTO> streamUploads(String accessToken, UUID submissionId) {
    return SecurityContextWrapper.forFlux(uploadService::getUploadStream)
        .apply(submissionId)
        .map(UploadDTO::fromDAO)
        .log("ApiController::streamUploads");
  }

  public ResponseEntity<Flux<DataBuffer>> download(List<UUID> objectIds) {
    return ResponseEntity.ok()
        .header(
            CONTENT_DISPOSITION_HEADER,
            format(
                "attachment; filename=sample-bundle-%s%s",
                Instant.now().toString(), FASTA_FILE_EXTENSION))
        .body(downloadsService.download(objectIds));
  }

  public ResponseEntity<Flux<DataBuffer>> downloadGzip(@RequestParam List<UUID> objectIds) {
    return ResponseEntity.ok()
        .header(
            CONTENT_DISPOSITION_HEADER,
            // convention for gzip is original file name with `.gz`
            format(
                "attachment; filename=sample-bundle-%s%s.gz",
                Instant.now().toString(), FASTA_FILE_EXTENSION))
        .body(downloadsService.download(objectIds).flatMap(this::gzipDataBuffer));
  }

  private Mono<DataBuffer> gzipDataBuffer(DataBuffer inputDataBuffer) {
    // allocate gzipped data buffer
    val gzipDataBuffer = new DefaultDataBufferFactory().allocateBuffer();
    try {
      // GZIPOutputStream basically decorates an OutputStream and allows writing bytes to it.
      // Since a spring DataBuffer can be obtained as an OutputStream,
      // we create a GZIPOutputStream around gzipDataBuffer and writes bytes from inputDataBuffer
      val gzip = new GZIPOutputStream(gzipDataBuffer.asOutputStream());
      val bytes = ByteStreams.toByteArray(inputDataBuffer.asInputStream());
      gzip.write(bytes);
      gzip.close();
    } catch (Exception e) {
      e.printStackTrace();
      return Mono.error(new UnknownException());
    }
    return Mono.just(gzipDataBuffer);
  }

  @ExceptionHandler
  public ResponseEntity<ErrorResponse> handle(Throwable ex) {
    log.error("ApiController exception handler", ex);
    if (ex instanceof MuseBaseException) {
      return ErrorResponse.errorResponseEntity((MuseBaseException) ex);
    } else {
      return ErrorResponse.errorResponseEntity(
          HttpStatus.INTERNAL_SERVER_ERROR, ex.getLocalizedMessage());
    }
  }

  private <T> Mono<EntityListResponse<T>> listResponseTransform(Mono<List<T>> entities) {
    return entities.map(entityList -> EntityListResponse.<T>builder().data(entityList).build());
  }
}
