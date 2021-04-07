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
import lombok.val;
import org.cancogenvirusseq.muse.repository.UploadRepository;
import org.cancogenvirusseq.muse.repository.model.Upload;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Optional;
import java.util.UUID;

@Service
public class UploadService {
  private final UploadRepository uploadRepository;

  public UploadService(@NonNull UploadRepository uploadRepository) {
    this.uploadRepository = uploadRepository;
  }

  public Flux<Upload> getUploadsPaged(
      Pageable page, Optional<UUID> submissionId, SecurityContext securityContext) {
    val userId = UUID.fromString(securityContext.getAuthentication().getName());
    return submissionId
        .map(id -> uploadRepository.findAllByUserIdAndSubmissionId(userId, id, page))
        .orElse(uploadRepository.findAllByUserId(userId, page));
  }

  public Flux<Upload> getUploads(Optional<UUID> submissionId, SecurityContext securityContext) {
    val userId = UUID.fromString(securityContext.getAuthentication().getName());
    return submissionId
        .map(id -> uploadRepository.findAllByUserIdAndSubmissionId(userId, id))
        .orElse(uploadRepository.findAllByUserId(userId));
  }
}
