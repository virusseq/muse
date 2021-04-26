package org.cancogenvirusseq.muse.components;

import static org.cancogenvirusseq.muse.components.ComponentTestStubs.*;
import static org.cancogenvirusseq.muse.components.FastaFileProcessor.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;

import lombok.val;
import org.cancogenvirusseq.muse.model.SubmissionUpload;
import org.junit.jupiter.api.Test;

public class FastaFileProcessorTests {

  @Test
  void testFileParsedCorrectly() {
    val fastaFile = STUB_FILE_1.getContent() + STUB_FILE_2.getContent();
    val subUpload = new SubmissionUpload("the.fasta", FASTA_TYPE, fastaFile);

    val fileMetaToSampleIdMap = processFileStrContent(subUpload);
    val expected = STUB_FILE_SAMPLE_MAP;
    assertEquals(expected, fileMetaToSampleIdMap);
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
