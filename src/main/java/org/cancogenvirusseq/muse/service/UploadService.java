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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.r2dbc.postgresql.PostgresqlConnectionFactory;
import io.r2dbc.postgresql.api.*;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.cancogenvirusseq.muse.repository.UploadRepository;
import org.cancogenvirusseq.muse.repository.model.Upload;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static java.lang.String.format;

@Slf4j
@Service
public class UploadService {
  private final UploadRepository uploadRepository;
  private final PostgresqlConnection connection;
  private final ObjectMapper objectMapper;

  public UploadService(
      @NonNull UploadRepository uploadRepository,
      @NonNull PostgresqlConnectionFactory connectionFactory,
      @NonNull ObjectMapper objectMapper) {
    this.uploadRepository = uploadRepository;
    this.objectMapper = objectMapper;
    // no need for .toFuture().get() here as constructors are allowed to block
    this.connection = Mono.from(connectionFactory.create()).block();
  }

  @PostConstruct
  private void postConstruct() {
    connection
        .createStatement("LISTEN upload_notification")
        .execute()
        .flatMap(PostgresqlResult::getRowsUpdated)
        .subscribe();
  }

  @PreDestroy
  private void preDestroy() {
    connection.close().subscribe();
  }

  public Flux<PostgresqlResult> batchCreatUploads(List<Upload> uploads) {
    // https://r2dbc.io/spec/0.8.0.RELEASE/spec/html/#statements.batching
    return Flux.fromIterable(uploads)
        .reduce(
            connection.createStatement("INSERT INTO upload(study_id, submitter_sample_id, submission_id, user_id, status, original_file_pair) VALUES ($1, $2, $3, $4, $5, $6)"),
            (acc, curr) -> {
              acc.bind("$1", curr.getStudyId());
              acc.bind("$2", curr.getSubmitterSampleId());
              acc.bind("$3", curr.getSubmissionId());
              acc.bind("$4", curr.getUserId());
              acc.bind("$5", curr.getStatus());
              acc.bind("$6", curr.getOriginalFilePair());
              
              return acc.add();
            })
        .flatMapMany(PostgresqlStatement::execute); // todo: cast this to Upload object?
  }

  public Flux<Upload> getUploadsPaged(
      Pageable page, UUID submissionId, SecurityContext securityContext) {
    val userId = UUID.fromString(securityContext.getAuthentication().getName());
    return Optional.ofNullable(submissionId)
        .map(id -> uploadRepository.findAllByUserIdAndSubmissionId(userId, id, page))
        .orElse(uploadRepository.findAllByUserId(userId, page));
  }

  public Flux<Upload> getUploadStream(UUID submissionId, SecurityContext securityContext) {
    return connection
        .getNotifications() // returns 🔥🔥🔥 HOT Flux 🔥🔥🔥
        .transform(this::transformToUploads)
        .transform(
            filterForUserAndMaybeSubmissionId(
                submissionId, securityContext.getAuthentication().getName()))
        .log("UploadService::getUploadStream");
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
