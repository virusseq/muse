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
import io.r2dbc.postgresql.api.Notification;
import io.r2dbc.postgresql.api.PostgresqlConnection;
import io.r2dbc.postgresql.api.PostgresqlResult;
import io.r2dbc.spi.ConnectionFactory;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import org.cancogenvirusseq.muse.repository.UploadRepository;
import org.cancogenvirusseq.muse.repository.model.Upload;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class UploadService {
  private final UploadRepository uploadRepository;
  private final PostgresqlConnection connection;
  private final ObjectMapper objectMapper;

  public UploadService(
      @NonNull UploadRepository uploadRepository,
      @NonNull ConnectionFactory connectionFactory,
      @NonNull ObjectMapper objectMapper) {
    this.uploadRepository = uploadRepository;
    this.objectMapper = objectMapper;
    // no need for .toFuture().get() here as constructors are allowed to block
    this.connection =
        Mono.from(connectionFactory.create()).cast(PostgresqlConnection.class).block();
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

  public Flux<Upload> getUploadsPaged(
      Pageable page, UUID submissionId, SecurityContext securityContext) {
    val userId = UUID.fromString(securityContext.getAuthentication().getName());
    return Optional.ofNullable(submissionId)
        .map(id -> uploadRepository.findAllByUserIdAndSubmissionId(userId, id, page))
        .orElse(uploadRepository.findAllByUserId(userId, page));
  }

  public Flux<Upload> getUploadStream(UUID submissionId, SecurityContext securityContext) {
    return connection
        .getNotifications()
        .map(Notification::getParameter)
        .map(this::uploadFromString)
        // filter for the JWT UUID from the security context
        .filter(
            upload ->
                upload.getUserId().toString().equals(securityContext.getAuthentication().getName()))
        // filter for the submissionID if provided otherwise ignore (filter always == true)
        .filter(
            upload ->
                Optional.ofNullable(submissionId)
                    .map(submissionIdVal -> submissionIdVal == upload.getSubmissionId())
                    .orElse(true));
  }

  @SneakyThrows
  private Upload uploadFromString(String uploadPayloadStr) {
    return objectMapper.readValue(uploadPayloadStr, Upload.class);
  }
}
