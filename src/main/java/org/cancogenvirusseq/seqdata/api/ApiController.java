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

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.cancogenvirusseq.seqdata.api.model.*;
import org.cancogenvirusseq.seqdata.service.DownloadService;
import org.cancogenvirusseq.seqdata.service.SubmitService;
import org.cancogenvirusseq.seqdata.service.UploadService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import reactor.core.publisher.Mono;

import javax.validation.Valid;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ApiController implements ApiDefinition {

  private final SubmitService submitService;
  private final UploadService uploadService;
  private final DownloadService downloadService;

  @PostMapping("/submit")
  public Mono<ResponseEntity<SubmitResponse>> submit(
      @NonNull @Valid @RequestBody SubmitRequest submitRequest) {
    return submitService.submit(submitRequest).map(this::respondOk);
  }

  @GetMapping("/uploads")
  public Mono<ResponseEntity<UploadListResponse>> getUploads(Integer pageSize, Integer pageToken) {
    val user = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    return uploadService.getUploads(user.getUsername(), pageSize, pageToken).map(this::respondOk);
  }

  @GetMapping("/uploads/{submitSetId}")
  public Mono<ResponseEntity<UploadListResponse>> getUploadsForSubmitSetId(
      @NonNull @PathVariable("submitSetId") String submitSetId,
      Integer pageSize,
      Integer pageToken) {
    val user = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    return uploadService
        .getUploadsForSubmitSetId(submitSetId, user.getUsername(), pageSize, pageToken)
        .map(this::respondOk);
  }

  @PostMapping("/download")
  public Mono<ResponseEntity<DownloadResponse>> download(
      @NonNull @Valid @RequestBody DownloadRequest downloadRequest) {
    return downloadService.download(downloadRequest).map(this::respondOk);
  }

  private <T> ResponseEntity<T> respondOk(T response) {
    return new ResponseEntity<T>(response, HttpStatus.OK);
  }
}
