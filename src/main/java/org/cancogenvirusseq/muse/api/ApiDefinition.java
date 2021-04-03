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

import io.swagger.annotations.*;
import java.nio.ByteBuffer;
import java.util.UUID;
import javax.validation.Valid;
import org.cancogenvirusseq.muse.api.model.*;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Api(value = "Molecular Upload Submission sErvice (Muse)", tags = "Muse")
public interface ApiDefinition {
  String BAD_REQUEST = "The request is malformed.";
  String UNAUTHORIZED_MSG = "Request requires authorization.";
  String FORBIDDEN_MSG = "The requester is not authorized to perform this action.";
  String UNKNOWN_MSG = "An unexpected error occurred.";

  @ApiOperation(
      value = "Get All Submissions",
      nickname = "Get Submissions",
      response = EntityListResponse.class,
      tags = "Muse")
  @ApiResponses(
      value = {
        @ApiResponse(code = 200, message = "", response = EntityListResponse.class),
        @ApiResponse(code = 400, message = BAD_REQUEST, response = ErrorResponse.class),
        @ApiResponse(code = 401, message = UNAUTHORIZED_MSG, response = ErrorResponse.class),
        @ApiResponse(code = 403, message = FORBIDDEN_MSG, response = ErrorResponse.class),
        @ApiResponse(code = 500, message = UNKNOWN_MSG, response = ErrorResponse.class)
      })
  @RequestMapping(
      value = "/submissions",
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE,
      method = RequestMethod.GET)
  Mono<ResponseEntity<EntityListResponse<SubmissionDTO>>> getSubmissions(
      @ApiParam(
              example = "10",
              value =
                  "OPTIONAL: The preferred number of entities to return for a page. If not provided, the implementation should use a default page size. The implementation must not return more items than `pageSize`, but it may return fewer.  Clients should not assume that if fewer than `pageSize` items are returned that all items have been returned.  The availability of additional pages is indicated by the value of `next_pageToken` in the response.")
          @Valid
          @RequestParam(value = "pageSize", defaultValue = "10", required = false)
          Integer pageSize,
      @ApiParam(
              example = "0",
              value =
                  "OPTIONAL: Token to use to indicate where to start getting results. If unspecified, return the first page of results.")
          @Valid
          @RequestParam(value = "pageToken", defaultValue = "0", required = false)
          Integer pageToken);

  @ApiOperation(
      value =
          "Submit a bundle of .tsv and .fasta files to process (exactly one .tsv and one or more fasta files)",
      nickname = "Submit",
      response = SubmissionCreateResponse.class,
      tags = "Muse")
  @ApiResponses(
      value = {
        @ApiResponse(code = 200, message = "", response = SubmissionCreateResponse.class),
        @ApiResponse(code = 401, message = UNAUTHORIZED_MSG, response = ErrorResponse.class),
        @ApiResponse(code = 403, message = FORBIDDEN_MSG, response = ErrorResponse.class),
        @ApiResponse(code = 500, message = UNKNOWN_MSG, response = ErrorResponse.class)
      })
  @RequestMapping(
      value = "/submissions",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE,
      method = RequestMethod.POST)
  Mono<ResponseEntity<SubmissionCreateResponse>> submit(
      // this isn't working correctly in the swagger-ui, know issue:
      // https://github.com/springfox/springfox/issues/3464
      @ApiParam(
              value =
                  "REQUIRED: Files to upload, must contain exactly one .tsv and one or more .fasta files, ex. example.tsv, example-data-1.fasta, example-data-2.fasta")
          @RequestPart("files")
          Flux<FilePart> fileParts);

  @ApiOperation(
      value = "Get All Uploads",
      nickname = "Get Uploads",
      response = EntityListResponse.class,
      tags = "Muse")
  @ApiResponses(
      value = {
        @ApiResponse(code = 200, message = "", response = EntityListResponse.class),
        @ApiResponse(code = 400, message = BAD_REQUEST, response = ErrorResponse.class),
        @ApiResponse(code = 401, message = UNAUTHORIZED_MSG, response = ErrorResponse.class),
        @ApiResponse(code = 403, message = FORBIDDEN_MSG, response = ErrorResponse.class),
        @ApiResponse(code = 500, message = UNKNOWN_MSG, response = ErrorResponse.class)
      })
  @RequestMapping(
      value = "/uploads",
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE,
      method = RequestMethod.GET)
  Mono<ResponseEntity<EntityListResponse<UploadDTO>>> getUploads(
      @ApiParam(
              example = "10",
              value =
                  "OPTIONAL: The preferred number of entities to return for a page. If not provided, the implementation should use a default page size. The implementation must not return more items than `pageSize`, but it may return fewer.  Clients should not assume that if fewer than `pageSize` items are returned that all items have been returned.  The availability of additional pages is indicated by the value of `next_pageToken` in the response.")
          @Valid
          @RequestParam(value = "pageSize", defaultValue = "10", required = false)
          Integer pageSize,
      @ApiParam(
              example = "0",
              value =
                  "OPTIONAL: Token to use to indicate where to start getting results. If unspecified, return the first page of results.")
          @Valid
          @RequestParam(value = "pageToken", defaultValue = "0", required = false)
          Integer pageToken,
      @ApiParam(
              example = "7fe7da94-bd30-4867-8a5e-042f6d9ccc48",
              value = "OPTIONAL: Filter list response by submissionId")
          @Valid
          @RequestParam(value = "submissionId", required = false)
          UUID submissionId);

  @ApiOperation(
      value = "Download molecular data as a single .fasta file",
      nickname = "Download",
      response = MultipartFile.class,
      tags = "Muse")
  @ApiResponses(
      value = {
        @ApiResponse(code = 200, message = "", response = MultipartFile.class),
        @ApiResponse(code = 401, message = UNAUTHORIZED_MSG, response = ErrorResponse.class),
        @ApiResponse(code = 403, message = FORBIDDEN_MSG, response = ErrorResponse.class),
        @ApiResponse(code = 500, message = UNKNOWN_MSG, response = ErrorResponse.class)
      })
  @RequestMapping(
      value = "/download",
      produces = MediaType.APPLICATION_OCTET_STREAM_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE,
      method = RequestMethod.GET)
  Mono<ResponseEntity<Flux<ByteBuffer>>> download(
      @Valid @RequestBody DownloadRequest downloadRequest);
}
