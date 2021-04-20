package org.cancogenvirusseq.muse.components;

import static org.cancogenvirusseq.muse.components.ComponentTestStubs.STUB_FILE_SAMPLE_MAP;
import static org.cancogenvirusseq.muse.components.FastaFileProcessor.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;

import lombok.val;
import org.junit.jupiter.api.Test;

public class FastaFileProcessorTests {

  @Test
  void testFileParsedCorrectly() {
    val fastaFile = ">ABCD/sam1/ddd/erd \n" + "CTGA \n" + ">EFG/sam2/ddd/erd \n" + "ATGC";

    val fileMetaToSampleIdMap = processFileStrContent(fastaFile);

    assertEquals(STUB_FILE_SAMPLE_MAP, fileMetaToSampleIdMap);
  }

  @Test
  void testBadFormatFileProcessed() {
    val fastaFile = "\n\thohoho>\tqucik";

    val fileMetaToSampleIdMap = processFileStrContent(fastaFile);

    assertThat(fileMetaToSampleIdMap, anEmptyMap());
  }

  @Test
  void testEmptyFileProcessed() {
    val fastaFile = "";

    val fileMetaToSampleIdMap = processFileStrContent(fastaFile);

    assertThat(fileMetaToSampleIdMap, anEmptyMap());
  }
}
