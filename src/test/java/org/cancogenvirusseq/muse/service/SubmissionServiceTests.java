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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.val;
import org.cancogenvirusseq.muse.exceptions.submission.SubmissionFilesException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import reactor.util.function.Tuples;

public class SubmissionServiceTests {

  final FilePart tsv = mock(FilePart.class);
  final FilePart fasta = mock(FilePart.class);
  final FilePart png = mock(FilePart.class);

  @BeforeEach
  void setUp() {
    when(tsv.filename()).thenReturn("test.tsv");
    when(fasta.filename()).thenReturn("test.fasta");
    when(png.filename()).thenReturn("test.png");
  }

  @Test
  void validateSubmissionSuccessTest() {
    val submission = Flux.just(tsv, fasta, fasta);

    val expected =
        new ConcurrentHashMap<>(Map.of("tsv", List.of(tsv), "fasta", List.of(fasta, fasta)));

    StepVerifier.create(SubmissionService.validateSubmission(submission))
        .expectNextMatches(actual -> actual.equals(expected))
        .verifyComplete();
  }

  @Test
  void validateSubmissionWrongFileExtensionTest() {
    val submission = Flux.just(png);

    StepVerifier.create(SubmissionService.validateSubmission(submission))
        .expectError(SubmissionFilesException.class)
        .verify();
  }

  @Test
  void validateSubmissionPartialWrongFileExtensionTest() {
    val submission = Flux.just(png, tsv, fasta, fasta);

    StepVerifier.create(SubmissionService.validateSubmission(submission))
        .expectError(SubmissionFilesException.class)
        .verify();
  }

  @Test
  void validateSubmissionMissingTsvTest() {
    val submission = Flux.just(fasta, fasta);

    StepVerifier.create(SubmissionService.validateSubmission(submission))
        .expectError(SubmissionFilesException.class)
        .verify();
  }

  @Test
  void validateSubmissionMissingFastaTest() {
    val submission = Flux.just(tsv);

    StepVerifier.create(SubmissionService.validateSubmission(submission))
        .expectError(SubmissionFilesException.class)
        .verify();
  }

  @Test
  void validateSubmissionMissingAllFilesTest() {
    Flux<FilePart> submission = Flux.empty();

    StepVerifier.create(SubmissionService.validateSubmission(submission))
        .expectError(SubmissionFilesException.class)
        .verify();
  }

  @Test
  void validateSubmissionTooManyTsvTest() {
    val submission = Flux.just(tsv, tsv, fasta, fasta);

    StepVerifier.create(SubmissionService.validateSubmission(submission))
        .expectError(SubmissionFilesException.class)
        .verify();
  }

  @Test
  void expandToFileTypeFilePartTupleTest() {
    StepVerifier.create(
            SubmissionService.expandToFileTypeFilePartTuple(
                Maps.immutableEntry("fasta", List.of(fasta, fasta))))
        .expectNext(Tuples.of("fasta", fasta))
        .expectNext(Tuples.of("fasta", fasta))
        .verifyComplete();
  }
}
