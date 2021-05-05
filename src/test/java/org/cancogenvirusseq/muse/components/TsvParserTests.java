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

import static java.util.stream.Collectors.toUnmodifiableList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import lombok.val;
import org.cancogenvirusseq.muse.components.security.Scopes;
import org.cancogenvirusseq.muse.config.websecurity.AuthProperties;
import org.cancogenvirusseq.muse.exceptions.submission.InvalidFieldsException;
import org.cancogenvirusseq.muse.exceptions.submission.InvalidHeadersException;
import org.cancogenvirusseq.muse.model.tsv_parser.InvalidField;
import org.cancogenvirusseq.muse.model.tsv_parser.TsvFieldSchema;
import org.junit.jupiter.api.Test;

public class TsvParserTests {

  private static final List<String> systemScopes = List.of("test.WRITE");

  private static final ImmutableList<TsvFieldSchema> TSV_SCHEMA =
      ImmutableList.of(
          new TsvFieldSchema("study_id", TsvFieldSchema.ValueType.string, true),
          new TsvFieldSchema("submitterId", TsvFieldSchema.ValueType.string, true),
          new TsvFieldSchema("name", TsvFieldSchema.ValueType.string, false),
          new TsvFieldSchema("age", TsvFieldSchema.ValueType.number, true));

  private final TsvParser parser;

  public TsvParserTests() {
    val authProperties = new AuthProperties();

    authProperties.getScopes().setSystem("test.WRITE");
    authProperties.getScopes().getStudy().setPrefix("muse.");
    authProperties.getScopes().getStudy().setSuffix(".WRITE");

    this.parser = new TsvParser(TSV_SCHEMA, new Scopes(authProperties));
  }

  @Test
  @SneakyThrows
  void testTsvStrParsedToRecords() {
    val tsvStr =
        "age\tname\tsubmitterId\tstudy_id\n"
            + "123\tconsensus_sequence\tQc-L00244359\tTEST-STUDY\n";
    val expected =
        List.of(
            Map.of(
                "submitterId",
                "Qc-L00244359",
                "name",
                "consensus_sequence",
                "age",
                "123",
                "study_id",
                "TEST-STUDY"));

    val actual =
        parser
            .parseAndValidateTsvStrToFlatRecords(tsvStr, systemScopes)
            .collect(toUnmodifiableList());

    assertThat(actual).hasSameElementsAs(expected);
  }

  @Test
  @SneakyThrows
  void testTsvStrParsedToRecordsWithStudyScope() {
    val tsvStr =
        "age\tname\tsubmitterId\tstudy_id\n"
            + "123\tconsensus_sequence\tQc-L00244359\tTEST-STUDY\n";
    val expected =
        List.of(
            Map.of(
                "submitterId",
                "Qc-L00244359",
                "name",
                "consensus_sequence",
                "age",
                "123",
                "study_id",
                "TEST-STUDY"));

    val actual =
        parser
            .parseAndValidateTsvStrToFlatRecords(tsvStr, List.of("muse.TEST-STUDY.WRITE"))
            .collect(toUnmodifiableList());

    assertThat(actual).hasSameElementsAs(expected);
  }

  @Test
  void testNullStudyPrefixScopeWorksCorrectly() {
    val nullPrefixAuthProperties = new AuthProperties();

    nullPrefixAuthProperties.getScopes().setSystem("test.WRITE");
    nullPrefixAuthProperties.getScopes().getStudy().setSuffix(".WRITE");

    val nullScopePrefixParser = new TsvParser(TSV_SCHEMA, new Scopes(nullPrefixAuthProperties));

    val tsvStr =
        "age\tname\tsubmitterId\tstudy_id\n"
            + "123\tconsensus_sequence\tQc-L00244359\tTEST-STUDY\n";
    val expected =
        List.of(
            Map.of(
                "submitterId",
                "Qc-L00244359",
                "name",
                "consensus_sequence",
                "age",
                "123",
                "study_id",
                "TEST-STUDY"));

    val actual =
        nullScopePrefixParser
            .parseAndValidateTsvStrToFlatRecords(tsvStr, List.of("muse.TEST-STUDY.WRITE"))
            .collect(toUnmodifiableList());

    assertThat(actual).hasSameElementsAs(expected);
  }

  @Test
  void testEmptyStudyPrefixScopeWorksCorrectly() {
    val emptyPrefixAuthProperties = new AuthProperties();

    emptyPrefixAuthProperties.getScopes().setSystem("test.WRITE");
    emptyPrefixAuthProperties.getScopes().getStudy().setPrefix("");
    emptyPrefixAuthProperties.getScopes().getStudy().setSuffix(".WRITE");

    val emptyScopePrefixParser = new TsvParser(TSV_SCHEMA, new Scopes(emptyPrefixAuthProperties));

    val tsvStr =
        "age\tname\tsubmitterId\tstudy_id\n"
            + "123\tconsensus_sequence\tQc-L00244359\tTEST-STUDY\n";
    val expected =
        List.of(
            Map.of(
                "submitterId",
                "Qc-L00244359",
                "name",
                "consensus_sequence",
                "age",
                "123",
                "study_id",
                "TEST-STUDY"));

    val actual =
        emptyScopePrefixParser
            .parseAndValidateTsvStrToFlatRecords(tsvStr, List.of("muse.TEST-STUDY.WRITE"))
            .collect(toUnmodifiableList());

    assertThat(actual).hasSameElementsAs(expected);
  }

  @Test
  void testErrorOnInvalidHeaders() {
    val tsvStr =
        "agee\tname\tsubmitterId\tstudy_id\n"
            + "123\tconsensus_sequence\tQc-L00244359\tTEST-STUDY\n";

    val thrown =
        assertThrows(
            InvalidHeadersException.class,
            () -> parser.parseAndValidateTsvStrToFlatRecords(tsvStr, systemScopes));

    assertThat(thrown.getMissingHeaders()).contains("age");
    assertThat(thrown.getUnknownHeaders()).contains("agee");
  }

  @Test
  void testErrorOnInvalidNumberType() {
    val tsvStr =
        "age\tname\tsubmitterId\tstudy_id\n"
            + "onetwothree\tconsensus_sequence\tQc-L00244359\tTEST-STUDY\n";

    val thrown =
        assertThrows(
            InvalidFieldsException.class,
            () -> parser.parseAndValidateTsvStrToFlatRecords(tsvStr, systemScopes));

    val expectedInvalidField =
        new InvalidField("age", "onetwothree", InvalidField.Reason.EXPECTING_NUMBER_TYPE, 1);

    assertThat(thrown.getInvalidFields()).contains(expectedInvalidField);
  }

  @Test
  void testErrorOnStudyNotAuthorized() {
    val tsvStr =
        "age\tname\tsubmitterId\tstudy_id\n"
            + "123\tconsensus_sequence\tQc-L00244359\tTEST-STUDY\n";

    val thrown =
        assertThrows(
            InvalidFieldsException.class,
            () ->
                parser.parseAndValidateTsvStrToFlatRecords(
                    tsvStr, List.of("muse.NOT-THIS-STUDY.WRITE")));

    val expectedInvalidField =
        new InvalidField(
            "study_id", "TEST-STUDY", InvalidField.Reason.UNAUTHORIZED_FOR_STUDY_UPLOAD, 1);

    assertThat(thrown.getInvalidFields()).contains(expectedInvalidField);
  }
}
