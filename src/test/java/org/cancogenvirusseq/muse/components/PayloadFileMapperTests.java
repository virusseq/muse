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

package org.cancogenvirusseq.muse.components;

import static org.assertj.core.api.Assertions.assertThat;
import static org.cancogenvirusseq.muse.components.ComponentTestStubs.*;
import static org.cancogenvirusseq.muse.components.FastaFileProcessor.FASTA_FILE_EXTENSION;
import static org.cancogenvirusseq.muse.components.FastaFileProcessor.FASTA_TYPE;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.SneakyThrows;
import lombok.val;
import org.cancogenvirusseq.muse.exceptions.submission.FoundInvalidFilesException;
import org.cancogenvirusseq.muse.exceptions.submission.MissingDataException;
import org.cancogenvirusseq.muse.model.SubmissionBundle;
import org.cancogenvirusseq.muse.model.SubmissionFile;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

public class PayloadFileMapperTests {

  private Authentication authentication = mock(Authentication.class);

  @Test
  @SneakyThrows
  void testPayloadsMappedToFiles() {
    val mapper = new ObjectMapper();
    val expectedSam1Payload = mapper.readValue(EXPECTED_SAM1_PAYLOAD, ObjectNode.class);
    val expectedSam2Payload = mapper.readValue(EXPECTED_SAM2_PAYLOAD, ObjectNode.class);

    val submissionBundle = new SubmissionBundle(authentication);
    submissionBundle.getFiles().putAll(STUB_FILE_SAMPLE_MAP);
    submissionBundle.getRecords().addAll(STUB_RECORDS);
    submissionBundle.getOriginalFileNames().addAll(Set.of("asdf.tsv", "the.fasta"));

    val fileMapper = new PayloadFileMapper(STUB_PAYLOAD_TEMPLATE);
    val actual = fileMapper.submissionBundleToSubmissionRequests(submissionBundle);

    assertThat(actual.get(0).getRecord()).isEqualTo(expectedSam1Payload);
    assertThat(actual.get(1).getRecord()).isEqualTo(expectedSam2Payload);

    assertThat(actual.get(0).getSubmissionFile()).isEqualTo(STUB_FILE_1);
    assertThat(actual.get(1).getSubmissionFile()).isEqualTo(STUB_FILE_2);

    assertThat(actual.get(0).getOriginalFileNames()).isEqualTo(Set.of("asdf.tsv", "the.fasta"));
    assertThat(actual.get(1).getOriginalFileNames()).isEqualTo(Set.of("asdf.tsv", "the.fasta"));

    assertThat(actual.get(0).getStudyId()).isEqualTo("TEST");
    assertThat(actual.get(1).getStudyId()).isEqualTo("TEST");
  }

  @Test
  @SneakyThrows
  void testErrorOnFailedToMapRecordsAndFile() {
    val records =
        List.of(
            Map.of("submitter id", "sam1", "isolate", "ABCD/sam1/ddd/erd", "age", "123"),
            Map.of("submitter id", "sam2NotHere", "isolate", "notHere", "age", "456"));

    val submissionBundle = new SubmissionBundle(authentication);
    submissionBundle.getFiles().putAll(STUB_FILE_SAMPLE_MAP);
    submissionBundle.getRecords().addAll(records);
    submissionBundle.getOriginalFileNames().addAll(Set.of("asdf.tsv", "the2.fasta"));

    val fileMapper = new PayloadFileMapper(STUB_PAYLOAD_TEMPLATE);
    val thrown =
        assertThrows(
            MissingDataException.class,
            () -> fileMapper.submissionBundleToSubmissionRequests(submissionBundle));

    assertThat(thrown.getIsolateInFileMissingInTsv()).containsExactly(ISOLATE_2);
    assertThat(thrown.getIsolateInRecordMissingInFile()).containsExactly("notHere");
  }

  @Test
  @SneakyThrows
  void testErrorOnFilesWithOnlyHeader() {
    val emptyFiles =
        Map.of(
            "EFG/sam2/ddd/erd",
            SubmissionFile.builder()
                .fileExtension(FASTA_FILE_EXTENSION)
                .fileSize(18)
                .fileMd5sum("d41d8cd98f00b204e9800998ecf8427e")
                .content(">EFG/sam2/ddd/erd\n")
                .fileType(FASTA_TYPE)
                .dataType(FASTA_TYPE)
                .submittedFileName("the.fasta")
                .build());

    val records =
        List.of(Map.of("submitter id", "sam2", "isolate", "EFG/sam2/ddd/erd", "age", "123"));

    val submissionBundle = new SubmissionBundle(authentication);
    submissionBundle.getFiles().putAll(emptyFiles);
    submissionBundle.getRecords().addAll(records);
    submissionBundle.getOriginalFileNames().addAll(Set.of("asdf.tsv", "the2.fasta"));

    val fileMapper = new PayloadFileMapper(STUB_PAYLOAD_TEMPLATE);
    val thrown =
        assertThrows(
            FoundInvalidFilesException.class,
            () -> fileMapper.submissionBundleToSubmissionRequests(submissionBundle));

    assertThat(thrown.getIsolateWithEmptyData()).containsExactly("EFG/sam2/ddd/erd");
  }
}
