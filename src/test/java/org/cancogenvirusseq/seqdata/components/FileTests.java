package org.cancogenvirusseq.seqdata.components;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.SneakyThrows;
import lombok.val;
import org.cancogenvirusseq.seqdata.model.FileMeta;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.cancogenvirusseq.seqdata.components.FileProcessor.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class FileTests {
    String sample1 = "sam1";
    String sample2 = "sam2";

    FileMeta file1Meta =
            FileMeta.builder()
                    .fileMd5sum("f433d470a7bacc3bdcdafeb1a4b4d758")
                    .fileName(sample1 + FASTA_FILE_EXTENSION)
                    .fileSize(26)
                    .fileType(FASTA_TYPE)
                    .dataCategory(FASTA_TYPE)
                    .dataType(FASTA_TYPE)
                    .build();

    FileMeta file2Meta =
            FileMeta.builder()
                    .fileMd5sum("eecf3de7e1136d99fffdd781d76bc81a")
                    .fileName(sample2 + FASTA_FILE_EXTENSION)
                    .fileSize(23)
                    .fileType(FASTA_TYPE)
                    .dataCategory(FASTA_TYPE)
                    .dataType(FASTA_TYPE)
                    .build();

    ConcurrentHashMap<String, FileMeta> expectedMap = new ConcurrentHashMap<>(Map.of(sample1, file1Meta, sample2, file2Meta));

    @Test
    void testWriteToFileAndMeta() {
        val fastaFile = ">ABCD/sam1/ddd/erd \n" + "CTGA \n" + ">EFG/sam2/ddd/erd \n" + "ATGC";

        val fileMetaToSampleIdMap = processFileStrContent(fastaFile);


        assertEquals(expectedMap, fileMetaToSampleIdMap);
    }


    @Test
    @SneakyThrows
    void testPartialPayloadsMappedToFiles() {
        val fastaFile = ">ABCD/sam1/ddd/erd \n" + "CTGA \n" + ">EFG/sam2/ddd/erd \n" + "ATGC";

        val partialPayload1 = "{ \"samples\": [ {\"submitterSampleId\": \"sam1\"}] }";
        val partialPayload2 = "{ \"samples\": [ {\"submitterSampleId\": \"sam2\"}] }";

        val finalPayload1 =
                "{ \"samples\": [ {\"submitterSampleId\": \"sam1\"}], "
                        + "\"files\":[{\"fileName\":\"sam1.v0.fasta\",\"fileSize\":26,\"fileMd5sum\":\"f433d470a7bacc3bdcdafeb1a4b4d758\",\"fileAccess\":\"OPEN\",\"fileType\":\"FASTA\",\"dataType\":\"FASTA\",\"info\":{\"data_category\":\"FASTA\"}}]"
                        + "}"
                        + "";

        val finalPayload2 =
                "{ \"samples\": [ {\"submitterSampleId\": \"sam2\"}], "
                        + "\"files\":[{\"fileName\":\"sam2.v0.fasta\",\"fileSize\":23,\"fileMd5sum\":\"eecf3de7e1136d99fffdd781d76bc81a\",\"fileAccess\":\"OPEN\",\"fileType\":\"FASTA\",\"dataType\":\"FASTA\",\"info\":{\"data_category\":\"FASTA\"}}]"
                        + "}"
                        + "";

        val mapper = new ObjectMapper();
        val jsonPP1 = mapper.readValue(partialPayload1, ObjectNode.class);
        val jsonPP2 = mapper.readValue(partialPayload2, ObjectNode.class);

        val jsonFPP1 = mapper.readValue(finalPayload1, ObjectNode.class);
        val jsonFPP2 = mapper.readValue(finalPayload2, ObjectNode.class);

        val fastaMapper = new FilePayloadMapper(expectedMap);
        val source = fastaMapper.apply(List.of(jsonPP1, jsonPP2));

        assertEquals(source.get(0), jsonFPP1);
        assertEquals(source.get(1), jsonFPP2);
    }
}
