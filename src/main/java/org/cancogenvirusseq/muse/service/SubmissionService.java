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

import static java.util.stream.Collectors.groupingByConcurrent;
import static org.cancogenvirusseq.muse.components.FastaFileProcessor.processFileStrContent;
import static org.cancogenvirusseq.muse.utils.SecurityContextWrapper.getUserIdFromContext;
import static org.springframework.core.io.buffer.DataBufferUtils.readInputStream;

import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.cancogenvirusseq.muse.api.model.SubmissionCreateResponse;
import org.cancogenvirusseq.muse.components.PayloadFileMapper;
import org.cancogenvirusseq.muse.components.TsvParser;
import org.cancogenvirusseq.muse.components.security.Scopes;
import org.cancogenvirusseq.muse.exceptions.submission.SubmissionFileGzipException;
import org.cancogenvirusseq.muse.exceptions.submission.SubmissionFilesException;
import org.cancogenvirusseq.muse.model.SubmissionBundle;
import org.cancogenvirusseq.muse.model.SubmissionUpload;
import org.cancogenvirusseq.muse.model.UploadEvent;
import org.cancogenvirusseq.muse.model.UploadRequest;
import org.cancogenvirusseq.muse.repository.SubmissionRepository;
import org.cancogenvirusseq.muse.repository.model.Submission;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubmissionService {

  private static final String METADATA_FILE_EXT = "tsv";
  private static final Set<String> MOLECULAR_FILE_EXT_SET = Set.of("fasta", "fa", "gz");
  private static final String OK_FILE_EXT_REGEX = "^.*\\.(tsv|fasta|fa|fasta\\.gz|fa\\.gz)$";

  private final Scopes scopes;
  private final SubmissionRepository submissionRepository;
  final UploadService uploadService;
  private final Sinks.Many<UploadEvent> songScoreSubmitUploadSink;
  private final TsvParser tsvParser;
  private final PayloadFileMapper payloadFileMapper;

  public Mono<Submission> getSubmissionById(
      @NonNull UUID submissionId, @NonNull SecurityContext securityContext) {
    return submissionRepository.getSubmissionByUserIdAndSubmissionId(
        UUID.fromString(securityContext.getAuthentication().getName()), submissionId);
  }

  /**
   * Retrieves submission entities from the database
   *
   * @param page - Pageable specification for response
   * @param securityContext - userId is extracted from the security context and used in the
   *     repository query
   * @return flux of submissions for the given user adhering to the pageable specification
   */
  public Flux<Submission> getSubmissions(
      @NonNull Pageable page, @NonNull SecurityContext securityContext) {
    return submissionRepository.findAllByUserId(
        UUID.fromString(securityContext.getAuthentication().getName()), page);
  }

  /**
   * Creates a new submission for Muse to queue and eventually process
   *
   * @param fileParts - files uploaded (should be exactly 1 .tsv and 1 or more .fasta)
   * @param securityContext - userId is extracted from the security context for submission
   * @return on success a SubmissionCreateResponse is returned, otherwise one of the exceptions that
   *     extend MuseBaseException is returned and eventually handled in the ApiController
   */
  public Mono<SubmissionCreateResponse> submit(
      @NonNull Flux<FilePart> fileParts, @NonNull SecurityContext securityContext) {
    return validateSubmission(fileParts)
        // extract to entry set
        .flatMapIterable(Map::entrySet)
        // flatten entry set lists to pair of filetype and FilePart
        .flatMap(SubmissionService::expandToFileTypeFilePartTuple)
        // read each file in as String
        .transform(SubmissionService::readFileContentToString)
        // reduce flux of Tuples(fileType, fileString) into a SubmissionRequest
        .reduce(
            new SubmissionBundle(securityContext.getAuthentication()),
            this::reduceToSubmissionBundle)
        // validate submission records has fasta file map!
        .map(payloadFileMapper::submissionBundleToSubmissionRequests)
        // record submission to database
        .flatMapMany(getPersistAndGenerateUploadEventsFunc(securityContext))
        // emit submission event to sink for further processing
        .doOnNext(songScoreSubmitUploadSink::tryEmitNext)
        // take the last uploadEvent and extract the submissionId
        .last()
        .map(
            uploadEvent ->
                new SubmissionCreateResponse(uploadEvent.getUpload().getSubmissionId().toString()));
  }

  /**
   * Splits the files into groups by filetype (extension in filename), verifies that there is
   * exactly one .tsv and 'one or more' .fasta files. Returns a Mono containing the file split map
   *
   * @param fileParts input flux of file parts
   * @return a Mono of Map containing files (value) grouped by type (key) that meet the validation
   *     requirements
   */
  public static Mono<ConcurrentMap<String, List<FilePart>>> validateSubmission(
      Flux<FilePart> fileParts) {
    return fileParts
        .collect(
            groupingByConcurrent(
                part ->
                    Optional.of(part.filename())
                        .filter(f -> f.matches(OK_FILE_EXT_REGEX))
                        .map(
                            f ->
                                MOLECULAR_FILE_EXT_SET.stream().anyMatch(f::endsWith)
                                    ? "molecular"
                                    : "meta")
                        .orElse("invalid")))
        .flatMap(
            fileTypeMap -> {
              // validate that we have exactly one metadata file and one or more molecular files
              if (fileTypeMap.getOrDefault("meta", Collections.emptyList()).size() == 1
                  && fileTypeMap.getOrDefault("molecular", Collections.emptyList()).size() >= 1
                  && fileTypeMap.keySet().equals(Set.of("meta", "molecular"))) {
                return Mono.just(fileTypeMap);
              } else {
                return Mono.error(
                    new SubmissionFilesException(
                        fileTypeMap.values().stream()
                            .flatMap(List::stream)
                            .map(FilePart::filename)
                            .collect(Collectors.toList())));
              }
            });
  }

  static Flux<Tuple2<String, FilePart>> expandToFileTypeFilePartTuple(
      Map.Entry<String, List<FilePart>> mapEntry) {
    return Flux.fromIterable(mapEntry.getValue())
        .map(
            filePart ->
                Optional.of(Pattern.compile(OK_FILE_EXT_REGEX).matcher(filePart.filename()))
                    .filter(Matcher::matches)
                    .map(matcher -> Tuples.of(matcher.group(1), filePart))
                    .orElseThrow());
  }

  /**
   * For each file submitted, extract the file contents into a string
   *
   * @param fileTypeFilePartTupleFlux - this tuple contains the pair of fileType as string and
   *     FilePart which represents one entire file from the multipart file upload
   * @return a flux of SubmissionUpload
   */
  private static Flux<SubmissionUpload> readFileContentToString(
      Flux<Tuple2<String, FilePart>> fileTypeFilePartTupleFlux) {
    return fileTypeFilePartTupleFlux.flatMap(
        fileTypeFilePart ->
            fileContentMaybeZippedToString
                .apply(fileTypeFilePart)
                .map(
                    fileStr ->
                        new SubmissionUpload(
                            fileTypeFilePart.getT2().filename(),
                            fileTypeFilePart.getT1(),
                            fileStr)));
  }

  private static final Function<Flux<DataBuffer>, Mono<String>> fileContentToString =
      (Flux<DataBuffer> content) ->
          content
              .map(
                  dataBuffer -> {
                    val bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);

                    return new String(bytes, StandardCharsets.UTF_8);
                  })
              .reduce(new StringBuilder(), StringBuilder::append)
              .map(StringBuilder::toString);

  private static final Function<Tuple2<String, FilePart>, Flux<DataBuffer>>
      getContentFromMaybeZipped =
          (Tuple2<String, FilePart> fileTypeFilePart) ->
              Optional.of(fileTypeFilePart)
                  .filter(f -> f.getT1().endsWith("gz"))
                  .map(
                      zippedTuple ->
                          zippedTuple
                              .getT2()
                              .content()
                              .map(DataBuffer::asInputStream)
                              .reduce(SubmissionService::reduceInputStreams)
                              .flatMapMany(
                                  inputStream ->
                                      readInputStream(
                                          () -> new GZIPInputStream(inputStream),
                                          new DefaultDataBufferFactory(),
                                          // 1024 because from observation of other
                                          // databuffers, this seems to be the size
                                          // they use
                                          1024))
                              .onErrorMap(
                                  throwable ->
                                      new SubmissionFileGzipException(
                                          zippedTuple.getT2().filename())))
                  .orElse(fileTypeFilePart.getT2().content());

  private static final Function<Tuple2<String, FilePart>, Mono<String>>
      fileContentMaybeZippedToString = getContentFromMaybeZipped.andThen(fileContentToString);

  @SneakyThrows
  private static InputStream reduceInputStreams(
      InputStream sequenceInputStream, InputStream inputStream) {
    val nextStream = new SequenceInputStream(sequenceInputStream, inputStream);
    inputStream.close();
    return nextStream;
  }

  private static Set<String> compileOriginalFilenames(Collection<UploadRequest> uploadRequests) {
    return uploadRequests.stream()
        .map(UploadRequest::getOriginalFileNames)
        .flatMap(Collection::stream)
        .collect(Collectors.toSet());
  }

  private static Set<String> compileStudyIds(Collection<UploadRequest> uploadRequests) {
    return uploadRequests.stream().map(UploadRequest::getStudyId).collect(Collectors.toSet());
  }

  private SubmissionBundle reduceToSubmissionBundle(
      SubmissionBundle submissionBundle, SubmissionUpload submissionUpload) {
    // append original filename
    submissionBundle.getOriginalFileNames().add(submissionUpload.getFilename());

    if (submissionUpload.getType().equals(METADATA_FILE_EXT)) {
      // parse and validate records from metadata file
      scopes
          .wrapWithUserScopes(
              tsvParser::parseAndValidateTsvStrToFlatRecords,
              submissionBundle.getUserAuthentication())
          .apply(submissionUpload.getContent())
          .forEach(record -> submissionBundle.getRecords().add(record));
    } else {
      // process the submitted file into upload ready files
      submissionBundle.getFiles().putAll(processFileStrContent(submissionUpload));
    }

    return submissionBundle;
  }

  private Function<Map<String, UploadRequest>, Publisher<? extends UploadEvent>>
      getPersistAndGenerateUploadEventsFunc(SecurityContext securityContext) {
    return submissionRequest ->
        submissionRepository
            .save(
                Submission.builder()
                    .userId(getUserIdFromContext(securityContext))
                    .createdAt(OffsetDateTime.now())
                    .originalFileNames(compileOriginalFilenames(submissionRequest.values()))
                    .studyIds(compileStudyIds(submissionRequest.values()))
                    .totalRecords(submissionRequest.size())
                    .build())
            // after we get a submissionId, batch create the uploads to be processed
            .flatMap(
                submission ->
                    uploadService.batchCreateUploadsFromSubmissionEvent(
                        submissionRequest.values(), submission.getSubmissionId(), securityContext))
            // extract list into flux
            .flatMapMany(Flux::fromIterable)
            // create UploadEvent from each upload and the submissionRequest
            .map(
                upload ->
                    UploadEvent.builder()
                        .studyId(upload.getStudyId())
                        .upload(upload)
                        .submissionFile(
                            submissionRequest.get(upload.getCompositeId()).getSubmissionFile())
                        .payload(
                            submissionRequest.get(upload.getCompositeId()).getRecord().toString())
                        .build())
            .log("SubmissionService::getPersistAndGenerateUploadEventsFunc", Level.FINE);
  }
}
