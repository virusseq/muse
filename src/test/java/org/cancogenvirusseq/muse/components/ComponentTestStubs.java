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
  public static final String SAMPLE_ID_1 = "sam1";
  public static final String SAMPLE_ID_2 = "sam2";

  public static final SubmissionFile STUB_FILE_1 =
      SubmissionFile.builder()
          .fileName(SAMPLE_ID_1 + FASTA_FILE_EXTENSION)
          .fileSize(26)
          .fileMd5sum("f433d470a7bacc3bdcdafeb1a4b4d758")
          .content(">ABCD/sam1/ddd/erd \nCTGA \n")
          .fileType(FASTA_TYPE)
          .dataType(FASTA_TYPE)
          .build();

  public static final SubmissionFile STUB_FILE_2 =
      SubmissionFile.builder()
          .fileName(SAMPLE_ID_2 + FASTA_FILE_EXTENSION)
          .fileSize(23)
          .fileMd5sum("eecf3de7e1136d99fffdd781d76bc81a")
          .content(">EFG/sam2/ddd/erd \nATGC")
          .fileType(FASTA_TYPE)
          .dataType(FASTA_TYPE)
          .build();

  public static final ConcurrentHashMap<String, SubmissionFile> STUB_FILE_SAMPLE_MAP =
      new ConcurrentHashMap<>(Map.of(SAMPLE_ID_1, STUB_FILE_1, SAMPLE_ID_2, STUB_FILE_2));

  public static final String STUB_PAYLOAD_TEMPLATE =
      "{ \"samples\": [ {\"submitterSampleId\": \"${submitter id}\"}]," + "\"age\": ${age} " + "}";

  public static final ArrayList<Map<String, String>> STUB_RECORDS =
      new ArrayList<>(
          List.of(
              Map.of("submitter id", "sam1", "age", "123"),
              Map.of("submitter id", "sam2", "age", "456")));
}
