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

import io.swagger.annotations.*;
import org.cancogenvirusseq.seqdata.api.model.DownloadRequest;
import org.cancogenvirusseq.seqdata.api.model.ErrorResponse;
import org.cancogenvirusseq.seqdata.api.model.SubmitResponse;
import org.cancogenvirusseq.seqdata.api.model.UploadListResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.nio.ByteBuffer;

@Api(value = "CanCoGenVirusSeq Data API", tags = "CanCoGen Virus Seq Data API")
public interface ApiDefinition {
  String BAD_REQUEST = "The request is malformed.";
  String UNAUTHORIZED_MSG = "Request requires authorization.";
  String FORBIDDEN_MSG = "The requester is not authorized to perform this action.";
  String UNKNOWN_MSG = "An unexpected error occurred.";

  @ApiOperation(
      value = "Submit a file pair of .tsv and .fasta to process",
      nickname = "Submit",
      response = SubmitResponse.class,
      tags = "CanCoGen Virus Seq Data API")
  @ApiResponses(
      value = {
        @ApiResponse(code = 200, message = "", response = SubmitResponse.class),
        @ApiResponse(code = 401, message = UNAUTHORIZED_MSG, response = ErrorResponse.class),
        @ApiResponse(code = 403, message = FORBIDDEN_MSG, response = ErrorResponse.class),
        @ApiResponse(code = 500, message = UNKNOWN_MSG, response = ErrorResponse.class)
      })
  @RequestMapping(
      value = "/submit",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE,
      method = RequestMethod.POST)
  Mono<ResponseEntity<SubmitResponse>> submit(@RequestPart("files") Flux<FilePart> filePartFlux);

  @ApiOperation(
      value = "Get All Uploads",
      nickname = "Get Uploads",
      response = UploadListResponse.class,
      tags = "CanCoGen Virus Seq Data API")
  @ApiResponses(
      value = {
        @ApiResponse(code = 200, message = "", response = UploadListResponse.class),
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
  Mono<ResponseEntity<UploadListResponse>> getUploads(
      @ApiParam(
              example = "10",
              value =
                  "OPTIONAL: The preferred number of uploads to return for a page. If not provided, the implementation should use a default page size. The implementation must not return more items than `pageSize`, but it may return fewer.  Clients should not assume that if fewer than `pageSize` items are returned that all items have been returned.  The availability of additional pages is indicated by the value of `next_pageToken` in the response.")
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
      value = "Get Uploads with the provided submitSetId",
      nickname = "Get Uploads for Submit Set",
      response = UploadListResponse.class,
      tags = "CanCoGen Virus Seq Data API")
  @ApiResponses(
      value = {
        @ApiResponse(code = 200, message = "", response = UploadListResponse.class),
        @ApiResponse(code = 400, message = BAD_REQUEST, response = ErrorResponse.class),
        @ApiResponse(code = 401, message = UNAUTHORIZED_MSG, response = ErrorResponse.class),
        @ApiResponse(code = 403, message = FORBIDDEN_MSG, response = ErrorResponse.class),
        @ApiResponse(code = 500, message = UNKNOWN_MSG, response = ErrorResponse.class)
      })
  @RequestMapping(
      value = "/uploads/{submitSetId}",
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE,
      method = RequestMethod.GET)
  Mono<ResponseEntity<UploadListResponse>> getUploadsForSubmitSetId(
      @ApiParam(value = "", required = true) @PathVariable("submitSetId") String submitSetId,
      @ApiParam(
              example = "10",
              value =
                  "OPTIONAL: The preferred number of uploads to return for a page. If not provided, the implementation should use a default page size. The implementation must not return more items than `pageSize`, but it may return fewer.  Clients should not assume that if fewer than `pageSize` items are returned that all items have been returned.  The availability of additional pages is indicated by the value of `next_pageToken` in the response.")
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
      value = "Download Virus Seq Data as a single .fasta file",
      nickname = "Download",
      response = MultipartFile.class,
      tags = "CanCoGen Virus Seq Data API")
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
