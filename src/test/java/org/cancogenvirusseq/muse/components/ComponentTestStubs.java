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

import static org.cancogenvirusseq.muse.components.FastaFileProcessor.FASTA_FILE_EXTENSION;
import static org.cancogenvirusseq.muse.components.FastaFileProcessor.FASTA_TYPE;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.experimental.UtilityClass;
import org.cancogenvirusseq.muse.model.SubmissionFile;

@UtilityClass
public class ComponentTestStubs {
  public static final String ISOLATE_1 = "ABCD/sam1/ddd/erd";
  public static final String ISOLATE_2 = "EFG/sam2/ddd/erd";

  public static final SubmissionFile STUB_FILE_1 =
      SubmissionFile.builder()
          .fileExtension(FASTA_FILE_EXTENSION)
          .fileSize(24)
          .fileMd5sum("cf20195497cc8c06075a6e201e82dd17")
          .content(">ABCD/sam1/ddd/erd \nCTGA")
          .fileType(FASTA_TYPE)
          .dataType(FASTA_TYPE)
          .submittedFileName("the.fasta")
          .build();

  public static final SubmissionFile STUB_FILE_2 =
      SubmissionFile.builder()
          .fileExtension(FASTA_FILE_EXTENSION)
          .fileSize(23)
          .fileMd5sum("eecf3de7e1136d99fffdd781d76bc81a")
          .content(">EFG/sam2/ddd/erd \nATGC")
          .fileType(FASTA_TYPE)
          .dataType(FASTA_TYPE)
          .submittedFileName("the.fasta")
          .build();

  public static final ConcurrentHashMap<String, SubmissionFile> STUB_FILE_SAMPLE_MAP =
      new ConcurrentHashMap<>(Map.of(ISOLATE_1, STUB_FILE_1, ISOLATE_2, STUB_FILE_2));

  public static final String STUB_PAYLOAD_TEMPLATE =
      "{\"studyId\": ${study_id}, "
          + "\"samples\": [ {\"submitterSampleId\": ${submitter id} }],"
          + "\"age\": ${age},"
          + "\"sample_collection\": { "
          + "\"isolate\": ${isolate}"
          + "}"
          + "}";

  public static final ArrayList<Map<String, String>> STUB_RECORDS =
      new ArrayList<>(
          List.of(
              Map.of(
                  "study_id",
                  "TEST-PR",
                  "submitter id",
                  "sam1",
                  "isolate",
                  "ABCD/sam1/ddd/erd",
                  "age",
                  "123"),
              Map.of(
                  "study_id",
                  "TEST-PR",
                  "submitter id",
                  "sam2",
                  "isolate",
                  "EFG/sam2/ddd/erd",
                  "age",
                  "456")));
}
