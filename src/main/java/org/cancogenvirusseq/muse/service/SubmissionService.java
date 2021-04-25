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

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.cancogenvirusseq.muse.api.model.SubmissionCreateResponse;
import org.cancogenvirusseq.muse.components.PayloadFileMapper;
import org.cancogenvirusseq.muse.components.TsvParser;
import org.cancogenvirusseq.muse.exceptions.submission.SubmissionFilesException;
import org.cancogenvirusseq.muse.model.SubmissionBundle;
import org.cancogenvirusseq.muse.model.SubmissionEvent;
import org.cancogenvirusseq.muse.model.SubmissionRequest;
import org.cancogenvirusseq.muse.model.SubmissionUpload;
import org.cancogenvirusseq.muse.repository.SubmissionRepository;
import org.cancogenvirusseq.muse.repository.model.Submission;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
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

  private final SubmissionRepository submissionRepository;
  private final Sinks.Many<SubmissionEvent> songScoreSubmitUploadSink;
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
        // reduce flux of SubmissionUpload to SubmissionBundle
        .reduce(new SubmissionBundle(), this::reduceToSubmissionBundle)
        // validate submission records has fasta file map and split to submissionRequests
        .map(payloadFileMapper::submissionBundleToSubmissionRequests)
        // record submission to database
        .flatMap(
            submissionRequest ->
                submissionRepository
                    .save(
                        Submission.builder()
                            .userId(getUserIdFromContext(securityContext))
                            .createdAt(OffsetDateTime.now())
                            .originalFileNames(compileOriginalFilenames(submissionRequest))
                            .totalRecords(submissionRequest.size())
                            .build())
                    // from recorded submission, create submissionEvent
                    .map(
                        submission ->
                            SubmissionEvent.builder()
                                .submissionId(submission.getSubmissionId())
                                .userId(getUserIdFromContext(securityContext))
                                .submissionRequests(submissionRequest)
                                .build()))
        // emit submission event to sink for further processing
        .doOnNext(songScoreSubmitUploadSink::tryEmitNext)
        // return submissionId to user
        .map(
            submissionEvent ->
                new SubmissionCreateResponse(submissionEvent.getSubmissionId().toString()));
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
                        .filter(f -> f.contains("."))
                        .map(f -> f.substring(part.filename().lastIndexOf(".") + 1))
                        .orElse("invalid")))
        .flatMap(
            fileTypeMap -> {
              // validate that we have exactly one .tsv and one or more fasta files
              if (fileTypeMap.getOrDefault("tsv", Collections.emptyList()).size() == 1
                  && fileTypeMap.getOrDefault("fasta", Collections.emptyList()).size() >= 1
                  && fileTypeMap.keySet().equals(Set.of("tsv", "fasta"))) {
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
        .map(filePart -> Tuples.of(mapEntry.getKey(), filePart));
  }

  private static Flux<SubmissionUpload> readFileContentToString(
      Flux<Tuple2<String, FilePart>> fileTypeFilePartTupleFlux) {
    return fileTypeFilePartTupleFlux.flatMap(
        fileTypeFilePart ->
            fileContentToString(fileTypeFilePart.getT2().content())
                .map(
                    fileStr ->
                        new SubmissionUpload(
                            fileTypeFilePart.getT2().filename(),
                            fileTypeFilePart.getT1(),
                            fileStr)));
  }

  private static Mono<String> fileContentToString(Flux<DataBuffer> content) {
    return content
        .map(
            dataBuffer -> {
              val bytes = new byte[dataBuffer.readableByteCount()];
              dataBuffer.read(bytes);
              DataBufferUtils.release(dataBuffer);

              return new String(bytes, StandardCharsets.UTF_8);
            })
        .reduce(String::concat);
  }

  private static Set<String> compileOriginalFilenames(List<SubmissionRequest> submissionRequests) {
    return submissionRequests.stream()
        .map(SubmissionRequest::getOriginalFileNames)
        .flatMap(Collection::stream)
        .collect(Collectors.toSet());
  }

  private SubmissionBundle reduceToSubmissionBundle(
      SubmissionBundle submissionBundle, SubmissionUpload submissionUpload) {
    // append original filename
    submissionBundle.getOriginalFileNames().add(submissionUpload.getFilename());

    switch (submissionUpload.getType()) {
      case "tsv":
        // parse and validate records
        tsvParser
            .parseAndValidateTsvStrToFlatRecords(submissionUpload.getContent())
            .forEach(record -> submissionBundle.getRecords().add(record));
        break;
      case "fasta":
        // process the submitted file into upload ready files
        submissionBundle.getFiles().putAll(processFileStrContent(submissionUpload));
        break;
    }
    return submissionBundle;
  }
}
