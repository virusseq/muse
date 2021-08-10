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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.experimental.UtilityClass;
import org.cancogenvirusseq.muse.model.SubmissionFile;

@UtilityClass
public class ComponentTestStubs {
  public static final String STUB_RECORD_0_FASTA_HEADER = "ABCD/sam1/ddd/erd";
  public static final String STUB_RECORD_1_FASTA_HEADER = "EFG/sam2/ddd/erd";

  public static final SubmissionFile STUB_FILE_0 =
      SubmissionFile.builder()
          .fileExtension(FASTA_FILE_EXTENSION)
          .fileSize(24)
          .fileMd5sum("cf20195497cc8c06075a6e201e82dd17")
          .content(">ABCD/sam1/ddd/erd \nCTGA")
          .fileType(FASTA_TYPE)
          .dataType(FASTA_TYPE)
          .submittedFileName("the.fasta")
          .build();

  public static final SubmissionFile STUB_FILE_1 =
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
      new ConcurrentHashMap<>(
          ImmutableMap.of(
              STUB_RECORD_0_FASTA_HEADER, STUB_FILE_0, STUB_RECORD_1_FASTA_HEADER, STUB_FILE_1));

  public static final String STUB_PAYLOAD_TEMPLATE =
      "{\"studyId\": ${study_id}, "
          + "\"samples\": [ {\"submitterSampleId\": ${submitter id} }],"
          + "\"age\": ${age},"
          + "\"sample_collection\": { "
          + "\"fasta_header_name\": ${fasta header name}"
          + "}"
          + "}";

  public static final ArrayList<Map<String, Object>> STUB_RECORDS =
      new ArrayList<>(
          ImmutableList.of(
              ImmutableMap.of(
                  "study_id",
                  "TEST-PR",
                  "submitter id",
                  "sam1",
                  "fasta header name",
                  "ABCD/sam1/ddd/erd",
                  "age",
                  123),
              ImmutableMap.of(
                  "study_id",
                  "TEST-PR",
                  "submitter id",
                  "sam2",
                  "fasta header name",
                  "EFG/sam2/ddd/erd",
                  "age",
                  456)));

  public static final String STUB_RECORD_0_PAYLOAD =
      "{ \"studyId\": \"TEST-PR\", "
          + "\"samples\": [ {\"submitterSampleId\": \"sam1\"}], "
          + "\"age\":123, "
          + "\"sample_collection\": { "
          + "\"fasta_header_name\": \"ABCD/sam1/ddd/erd\""
          + "},"
          + "\"files\":[{\"fileName\":\"sam1.fasta\",\"fileSize\":24,\"fileMd5sum\":\"cf20195497cc8c06075a6e201e82dd17\",\"fileAccess\":\"open\",\"fileType\":\"FASTA\",\"dataType\":\"FASTA\"}]"
          + "}";

  public static final String STUB_RECORD_1_PAYLOAD =
      "{ \"studyId\": \"TEST-PR\", "
          + "\"samples\": [ {\"submitterSampleId\": \"sam2\"}], "
          + "\"age\":456, "
          + "\"sample_collection\": { "
          + "\"fasta_header_name\": \"EFG/sam2/ddd/erd\""
          + "},"
          + "\"files\":[{\"fileName\":\"sam2.fasta\",\"fileSize\":23,\"fileMd5sum\":\"eecf3de7e1136d99fffdd781d76bc81a\",\"fileAccess\":\"open\",\"fileType\":\"FASTA\",\"dataType\":\"FASTA\"}]"
          + "}";
}
