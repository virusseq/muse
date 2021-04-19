package org.cancogenvirusseq.muse.components;

import lombok.experimental.UtilityClass;
import org.cancogenvirusseq.muse.model.SubmissionBundle;
import org.cancogenvirusseq.muse.model.SubmissionFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.cancogenvirusseq.muse.components.FastaFileProcessor.FASTA_FILE_EXTENSION;
import static org.cancogenvirusseq.muse.components.FastaFileProcessor.FASTA_TYPE;

@UtilityClass
public class ComponentTestStubs {
    public static final String sampleId1 = "sam1";
    public static final String sampleId2 = "sam2";

    public static final SubmissionFile file1Meta =
            SubmissionFile.builder()
                    .fileName(sampleId1 + FASTA_FILE_EXTENSION)
                    .fileSize(26)
                    .fileMd5sum("f433d470a7bacc3bdcdafeb1a4b4d758")
                    .content(">ABCD/sam1/ddd/erd \nCTGA \n")
                    .fileType(FASTA_TYPE)
                    .dataType(FASTA_TYPE)
                    .build();

    public static final SubmissionFile file2Meta =
            SubmissionFile.builder()
                    .fileName(sampleId2 + FASTA_FILE_EXTENSION)
                    .fileSize(23)
                    .fileMd5sum("eecf3de7e1136d99fffdd781d76bc81a")
                    .content(">EFG/sam2/ddd/erd \nATGC")
                    .fileType(FASTA_TYPE)
                    .dataType(FASTA_TYPE)
                    .build();

    public static final ConcurrentHashMap<String, SubmissionFile> sampleIdToFileMeta =
            new ConcurrentHashMap<>(Map.of(sampleId1, file1Meta
//                    , sampleId2, file2Meta
            ));

    public static final String PAYLOAD_TEMPLATE = "{ \"samples\": [ {\"submitterSampleId\": \"${submitter id}\"}]," +
                                                       "\"age\": ${age} " +
                                                  "}";

    public static final ArrayList<Map<String, String>> EXPECTED_TSV_RECORDS = new ArrayList<>(List.of(Map.of("submitter id", "sam1", "age", "123")));
}
