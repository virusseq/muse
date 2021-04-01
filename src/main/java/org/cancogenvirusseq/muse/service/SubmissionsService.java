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

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.cancogenvirusseq.muse.api.model.SubmissionCreateResponse;
import org.cancogenvirusseq.muse.api.model.SubmissionListResponse;
import org.cancogenvirusseq.muse.model.SubmissionEvent;
import org.cancogenvirusseq.muse.model.SubmissionFile;
import org.cancogenvirusseq.muse.repository.SubmissionRepository;
import org.cancogenvirusseq.muse.repository.model.SubmissionDAO;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.util.function.Tuples;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.util.stream.Collectors.groupingByConcurrent;
import static org.cancogenvirusseq.muse.components.FastaFileProcessor.processFileStrContent;
import static org.cancogenvirusseq.muse.components.TsvParser.parseTsvStrToFlatRecords;

@Service
@Slf4j
public class SubmissionsService {

  public final SubmissionRepository submissionRepository;
  public final Sinks.Many<SubmissionEvent> submissionsSink;

  public SubmissionsService(@NonNull SubmissionRepository submissionRepository) {
    this.submissionRepository = submissionRepository;
    this.submissionsSink = Sinks.many().unicast().onBackpressureBuffer();
  }

  public Mono<SubmissionListResponse> getSubmissions(
      String userId, Integer pageSize, Integer pageToken) {
    return Mono.just(new SubmissionListResponse(Collections.emptyList()));
  }

  public Mono<SubmissionCreateResponse> submit(String userId, Flux<FilePart> fileParts) {
    return validateAndSplitSubmission(fileParts)
        // take validated map of fileType => filePartList and
        // convert to Flux Tuples(fileType, fileString)
        .flatMapMany(
            filePartsMap ->
                Flux.fromStream(
                    filePartsMap.entrySet().parallelStream()
                        .flatMap(
                            filePartsMapEntries ->
                                filePartsMapEntries.getValue().parallelStream()
                                    .map(
                                        filePart ->
                                            Tuples.of(filePartsMapEntries.getKey(), filePart)))))
        .flatMap(
            fileTypeFilePart ->
                fileContentToString(fileTypeFilePart.getT2().content())
                    .map(fileStr -> Tuples.of(fileTypeFilePart.getT1(), fileStr)))
        // reduce flux of Tuples(fileType, fileString) into a single tuple of (records, submissionFilesMap)
        .reduce(
            Tuples.of(
                new ArrayList<Map<String, String>>(),
                new ConcurrentHashMap<String, SubmissionFile>()),
            (recordsSubmissionFiles, fileTypeFileStr) -> {
              switch (fileTypeFileStr.getT1()) {
                case "tsv":
                  parseTsvStrToFlatRecords(fileTypeFileStr.getT2())
                      .forEach(record -> recordsSubmissionFiles.getT1().add(record));
                  break;
                case "fasta":
                  recordsSubmissionFiles
                      .getT2()
                      .putAll(processFileStrContent(fileTypeFileStr.getT2()));
                  break;
              }
              return recordsSubmissionFiles;
            })
        // record submission to database, create submissionEvent
        .flatMap(
            recordsSubmissionFiles ->
                fileParts
                    .map(FilePart::filename)
                    .collectList()
                    .flatMap(
                        fileList ->
                            submissionRepository
                                .save(
                                    SubmissionDAO.builder()
                                        .userId(UUID.fromString(userId))
                                        .createdAt(LocalDateTime.now())
                                        .originalFileNames(fileList)
                                        .totalRecords(recordsSubmissionFiles.getT1().size())
                                        .build())
                                // from recorded submission, create submissionEvent
                                .map(
                                    submission ->
                                        SubmissionEvent.builder()
                                            .submissionId(submission.getSubmissionId())
                                            .records(recordsSubmissionFiles.getT1())
                                            .submissionFilesMap(recordsSubmissionFiles.getT2())
                                            .build())))
        // emit submission event to sink for further processing
        .doOnNext(submissionsSink::tryEmitNext)
        // return submissionId to user
        .map(
            submissionEvent ->
                new SubmissionCreateResponse(submissionEvent.getSubmissionId().toString()));
  }

  /**
   * Splits the files into groups by filetype (extension in filename), verifies that there is
   * exactly one .tsv and 'one or more' .fasta files. Returns a flux containing the file split map
   *
   * @param fileParts input flux of file parts
   * @return a map containing files grouped by type that meet the validation requirements
   */
  private Mono<ConcurrentMap<String, List<FilePart>>> validateAndSplitSubmission(
      Flux<FilePart> fileParts) {
    return fileParts
        .collect(
            groupingByConcurrent(
                part ->
                    Optional.of(part.filename())
                        .filter(f -> f.contains("."))
                        .map(f -> f.substring(part.filename().lastIndexOf(".") + 1))
                        .orElse("invalid")))
        .handle(
            (fileTypeMap, sink) -> {
              // validate that we have exactly one .tsv and one or more fasta files
              if (fileTypeMap.getOrDefault("tsv", Collections.emptyList()).size() == 1
                  && fileTypeMap.getOrDefault("fasta", Collections.emptyList()).size() >= 1) {
                sink.next(fileTypeMap);
              } else {
                sink.error(
                    new IllegalArgumentException(
                        "Submission must contain exactly one .tsv file and one or more .fasta files"));
              }
            });
  }

  private Mono<String> fileContentToString(Flux<DataBuffer> content) {
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
}
