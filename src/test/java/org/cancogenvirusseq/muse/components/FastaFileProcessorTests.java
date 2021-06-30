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

import static org.cancogenvirusseq.muse.components.ComponentTestStubs.*;
import static org.cancogenvirusseq.muse.components.FastaFileProcessor.FASTA_TYPE;
import static org.cancogenvirusseq.muse.components.FastaFileProcessor.processFileStrContent;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;

import lombok.val;
import org.cancogenvirusseq.muse.model.SubmissionUpload;
import org.junit.jupiter.api.Test;

public class FastaFileProcessorTests {

  @Test
  void testFileParsedCorrectly() {
    val fastaFile = STUB_FILE_0.getContent() + STUB_FILE_1.getContent();
    val subUpload = new SubmissionUpload("the.fasta", FASTA_TYPE, fastaFile);

    val fileMetaToSampleIdMap = processFileStrContent(subUpload);
    assertEquals(STUB_FILE_SAMPLE_MAP, fileMetaToSampleIdMap);
  }

  @Test
  void testBadFormatFileProcessed() {
    val fastaFile = "\n\thohoho>\tqucik";
    val subUpload = new SubmissionUpload("the.fasta", FASTA_TYPE, fastaFile);

    val fileMetaToSampleIdMap = processFileStrContent(subUpload);

    assertThat(fileMetaToSampleIdMap, anEmptyMap());
  }

  @Test
  void testEmptyFileProcessed() {
    val subUpload = new SubmissionUpload("the.fasta", FASTA_TYPE, "");

    val fileMetaToSampleIdMap = processFileStrContent(subUpload);

    assertThat(fileMetaToSampleIdMap, anEmptyMap());
  }
}
