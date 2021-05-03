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

import static org.cancogenvirusseq.muse.utils.SecurityContextWrapper.getUserIdFromContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.r2dbc.postgresql.PostgresqlConnectionFactory;
import io.r2dbc.postgresql.api.Notification;
import io.r2dbc.postgresql.api.PostgresqlConnection;
import io.r2dbc.postgresql.api.PostgresqlResult;
import java.util.*;
import java.util.function.Function;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.cancogenvirusseq.muse.model.UploadRequest;
import org.cancogenvirusseq.muse.repository.UploadRepository;
import org.cancogenvirusseq.muse.repository.model.Upload;
import org.cancogenvirusseq.muse.repository.model.UploadStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class UploadService {
  private final UploadRepository uploadRepository;
  private final PostgresqlConnection uploadStreamConnection;
  private final ObjectMapper objectMapper;

  public UploadService(
      @NonNull UploadRepository uploadRepository,
      @NonNull PostgresqlConnectionFactory postgresqlConnectionFactory,
      @NonNull ObjectMapper objectMapper) {
    this.uploadRepository = uploadRepository;
    this.objectMapper = objectMapper;
    // no need for .toFuture().get() here as constructors are allowed to block
    this.uploadStreamConnection = Mono.from(postgresqlConnectionFactory.create()).block();
  }

  @PostConstruct
  private void postConstruct() {
    uploadStreamConnection
        .createStatement("LISTEN upload_notification")
        .execute()
        .flatMap(PostgresqlResult::getRowsUpdated)
        .subscribe();
  }

  @PreDestroy
  private void preDestroy() {
    uploadStreamConnection.close().subscribe();
  }

  /**
   * Batch create upload entities (status == QUEUED) given upload requests, submissionId, and a
   * security context. Note that this isn't true batching as that is not currently supported
   * (https://github.com/spring-projects/spring-data-r2dbc/issues/259), the saveAll method just uses
   * concatMap underneath, but this does accomplish our goals without resorting to raw SQL string
   * construction which is just not worth the risk.
   *
   * @param uploadRequests - list of uploads
   * @param submissionId - submissionId to associate uploads with
   * @param securityContext - security context from which userId is extracted and associated with
   *     each upload
   * @return resulting list of queued Uploads
   */
  public Mono<List<Upload>> batchCreateUploadsFromSubmissionEvent(
      Collection<UploadRequest> uploadRequests,
      UUID submissionId,
      SecurityContext securityContext) {
    val userId = getUserIdFromContext(securityContext);
    return uploadRepository
        .saveAll(
            Flux.fromIterable(uploadRequests)
                .map(
                    uploadRequest ->
                        Upload.builder()
                            .studyId(uploadRequest.getStudyId())
                            .submitterSampleId(uploadRequest.getSubmitterSampleId())
                            .submissionId(submissionId)
                            .userId(userId)
                            .status(UploadStatus.QUEUED)
                            .originalFilePair(uploadRequest.getOriginalFileNames())
                            .build()))
        .collectList()
        .doFinally(
            signalType -> {
              log.info("Batch create upload complete: {}", signalType);
            })
        .log("UploadService::batchUploadsFromSubmissionEvent");
  }

  public Flux<Upload> getUploadsPaged(
      Pageable page, UUID submissionId, SecurityContext securityContext) {
    val userId = getUserIdFromContext(securityContext);
    return Optional.ofNullable(submissionId)
        .map(id -> uploadRepository.findAllByUserIdAndSubmissionId(userId, id, page))
        .orElse(uploadRepository.findAllByUserId(userId, page));
  }

  public Flux<Upload> getUploadStream(UUID submissionId, SecurityContext securityContext) {
    return uploadStreamConnection
        .getNotifications() // returns 🔥🔥🔥 HOT Flux 🔥🔥🔥
        .transform(this::transformToUploads)
        .transform(
            filterForUserAndMaybeSubmissionId(
                submissionId, securityContext.getAuthentication().getName()))
        .log("UploadService::getUploadStream");
  }

  public Mono<Upload> updateUpload(Upload upload) {
    return uploadRepository.save(upload);
  }

  public static Function<Flux<Upload>, Flux<Upload>> filterForUserAndMaybeSubmissionId(
      UUID submissionId, String userId) {
    return (Flux<Upload> uploads) ->
        uploads
            // filter for the JWT UUID from the security context
            .filter(upload -> upload.getUserId().toString().equals(userId))
            // filter for the submissionID if provided otherwise ignore (filter always == true)
            .filter(
                upload ->
                    Optional.ofNullable(submissionId)
                        .map(submissionIdVal -> submissionIdVal.equals(upload.getSubmissionId()))
                        .orElse(true))
            .log("UploadService::filterForUserAndMaybeSubmissionId");
  }

  private Flux<Upload> transformToUploads(Flux<Notification> notifications) {
    return notifications
        .map(Notification::getParameter)
        .filter(Objects::nonNull)
        .map(this::uploadFromString)
        .log("UploadService::transformToUploads");
  }

  @SneakyThrows
  private Upload uploadFromString(String uploadPayloadStr) {
    return objectMapper.readValue(uploadPayloadStr, Upload.class);
  }
}
