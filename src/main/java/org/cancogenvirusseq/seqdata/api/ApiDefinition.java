package org.cancogenvirusseq.seqdata.api;

import io.swagger.annotations.*;
import org.cancogenvirusseq.seqdata.api.model.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.validation.Valid;

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
      produces = {"application/json"},
      consumes = {"application/json"},
      method = RequestMethod.POST)
  Mono<ResponseEntity<SubmitResponse>> submit(@Valid @RequestBody SubmitRequest submitRequest);

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
      produces = {"application/json"},
      consumes = {"application/json"},
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
      produces = {"application/json"},
      consumes = {"application/json"},
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
      response = DownloadResponse.class,
      tags = "CanCoGen Virus Seq Data API")
  @ApiResponses(
      value = {
        @ApiResponse(code = 200, message = "", response = DownloadResponse.class),
        @ApiResponse(code = 401, message = UNAUTHORIZED_MSG, response = ErrorResponse.class),
        @ApiResponse(code = 403, message = FORBIDDEN_MSG, response = ErrorResponse.class),
        @ApiResponse(code = 500, message = UNKNOWN_MSG, response = ErrorResponse.class)
      })
  @RequestMapping(
      value = "/download",
      produces = {"application/json"},
      consumes = {"application/json"},
      method = RequestMethod.GET) // todo: set correct contentType
  Mono<ResponseEntity<DownloadResponse>> download(
      @Valid @RequestBody DownloadRequest downloadRequest);
}
