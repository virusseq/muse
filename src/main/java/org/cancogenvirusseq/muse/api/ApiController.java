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

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cancogenvirusseq.muse.api.model.*;
import org.cancogenvirusseq.muse.service.DownloadsService;
import org.cancogenvirusseq.muse.service.SubmissionService;
import org.cancogenvirusseq.muse.service.UploadService;
import org.cancogenvirusseq.muse.utils.SecurityContextWrapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ApiController implements ApiDefinition {

  private final SubmissionService submissionService;
  private final UploadService uploadService;
  private final DownloadsService downloadsService;

  @GetMapping("/submissions")
  public Mono<ResponseEntity<EntityListResponse<SubmissionDTO>>> getSubmissions(
      Integer page, Integer size, Sort.Direction sortDirection, SubmissionSortField sortField) {
    return SecurityContextWrapper.forFlux(submissionService::getSubmissions)
        .apply(PageRequest.of(page, size, Sort.by(sortDirection, sortField.toString())))
        .map(SubmissionDTO::fromDAO)
        .collectList()
        .transform(this::listResponseTransform);
  }

  @PostMapping("/submissions")
  public Mono<ResponseEntity<SubmissionCreateResponse>> submit(
      @RequestPart("files") Flux<FilePart> fileParts) {
    return SecurityContextWrapper.forMono(submissionService::submit)
        .apply(fileParts)
        .map(this::respondOk);
  }

  @GetMapping("/uploads")
  public Mono<ResponseEntity<EntityListResponse<UploadDTO>>> getUploads(
      Integer page,
      Integer size,
      Sort.Direction sortDirection,
      UploadSortField sortField,
      UUID submissionId) {
    return SecurityContextWrapper.forFlux(uploadService::getUploads)
        .apply(
            PageRequest.of(page, size, Sort.by(sortDirection, sortField.toString())),
            Optional.ofNullable(submissionId))
        .map(UploadDTO::fromDAO)
        .collectList()
        .transform(this::listResponseTransform);
  }

  @PostMapping("/download")
  public Mono<ResponseEntity<Flux<ByteBuffer>>> download(
      @NonNull @Valid @RequestBody DownloadRequest downloadRequest) {
    return downloadsService.download(downloadRequest).map(this::respondOk);
  }

  @ExceptionHandler
  public ResponseEntity<ErrorResponse> handle(Throwable ex) {
    if (ex instanceof IllegalArgumentException) {
      return ErrorResponse.errorResponseEntity(HttpStatus.BAD_REQUEST, ex.getLocalizedMessage());
    } else {
      return ErrorResponse.errorResponseEntity(
          HttpStatus.INTERNAL_SERVER_ERROR, ex.getLocalizedMessage());
    }
  }

  private <T> ResponseEntity<T> respondOk(T response) {
    return new ResponseEntity<T>(response, HttpStatus.OK);
  }

  private <T> Mono<ResponseEntity<EntityListResponse<T>>> listResponseTransform(
      Mono<List<T>> entities) {
    return entities
        .map(entityList -> EntityListResponse.<T>builder().data(entityList).build())
        .map(this::respondOk);
  }
}
