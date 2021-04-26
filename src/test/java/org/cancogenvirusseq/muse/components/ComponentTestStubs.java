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
      "{"
          + "\"samples\": [ {\"submitterSampleId\": ${submitter id} }],"
          + "\"age\": ${age},"
          + "\"sample_collection\": { "
          + "\"isolate\": ${isolate}"
          + "}"
          + "}";

  public static final ArrayList<Map<String, String>> STUB_RECORDS =
      new ArrayList<>(
          List.of(
              Map.of("submitter id", "sam1", "isolate", "ABCD/sam1/ddd/erd", "age", "123"),
              Map.of("submitter id", "sam2", "isolate", "EFG/sam2/ddd/erd", "age", "456")));
}
