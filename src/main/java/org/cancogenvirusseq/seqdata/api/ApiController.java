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

package org.cancogenvirusseq.seqdata.api;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.UUID;
import javax.validation.Valid;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.cancogenvirusseq.seqdata.api.model.DownloadRequest;
import org.cancogenvirusseq.seqdata.api.model.SubmissionCreateResponse;
import org.cancogenvirusseq.seqdata.api.model.SubmissionListResponse;
import org.cancogenvirusseq.seqdata.api.model.UploadListResponse;
import org.cancogenvirusseq.seqdata.service.DownloadsService;
import org.cancogenvirusseq.seqdata.service.SubmissionsService;
import org.cancogenvirusseq.seqdata.service.UploadsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ApiController implements ApiDefinition {

  private final SubmissionsService submissionsService;
  private final UploadsService uploadsService;
  private final DownloadsService downloadsService;

  @GetMapping("/submissions")
  public Mono<ResponseEntity<SubmissionListResponse>> getSubmissions(
      Integer pageSize, Integer pageToken) {
    val user = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    return submissionsService
        .getSubmissions(user.getUsername(), pageSize, pageToken)
        .map(this::respondOk);
  }

  @PostMapping("/submissions")
  public Mono<ResponseEntity<SubmissionCreateResponse>> submit(
      @RequestPart("files") Flux<FilePart> fileParts) {
    return submissionsService.submit(fileParts).map(this::respondOk);
  }

  @GetMapping("/uploads")
  public Mono<ResponseEntity<UploadListResponse>> getUploads(
      Integer pageSize, Integer pageToken, UUID submissionId) {
    val user = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    return uploadsService
        .getUploads(user.getUsername(), pageSize, pageToken, Optional.ofNullable(submissionId))
        .map(this::respondOk);
  }

  @PostMapping("/download")
  public Mono<ResponseEntity<Flux<ByteBuffer>>> download(
      @NonNull @Valid @RequestBody DownloadRequest downloadRequest) {
    return downloadsService.download(downloadRequest).map(this::respondOk);
  }

  private <T> ResponseEntity<T> respondOk(T response) {
    return new ResponseEntity<T>(response, HttpStatus.OK);
  }
}
