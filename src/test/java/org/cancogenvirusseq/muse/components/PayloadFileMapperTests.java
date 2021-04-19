package org.cancogenvirusseq.muse.components;

import static org.assertj.core.api.Assertions.assertThat;
import static org.cancogenvirusseq.muse.components.ComponentTestStubs.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import lombok.SneakyThrows;
import lombok.val;
import org.cancogenvirusseq.muse.model.SubmissionBundle;
import org.cancogenvirusseq.muse.model.SubmissionRequest;
import org.junit.jupiter.api.Test;

public class PayloadFileMapperTests {

  @Test
  @SneakyThrows
  void testPayloadsMappedToFiles() {
    val sam1PayloadStr =
        "{ \"samples\": [ {\"submitterSampleId\": \"sam1\"}], "
            + "\"age\":123, "
            + "\"files\":[{\"fileName\":\"sam1.fasta\",\"fileSize\":26,\"fileMd5sum\":\"f433d470a7bacc3bdcdafeb1a4b4d758\",\"fileAccess\":\"open\",\"fileType\":\"FASTA\",\"dataType\":\"FASTA\"}]"
            + "}";
    val sam2PayloadStr =
        "{ \"samples\": [ {\"submitterSampleId\": \"sam2\"}], "
            + "\"age\":456, "
            + "\"files\":[{\"fileName\":\"sam2.fasta\",\"fileSize\":23,\"fileMd5sum\":\"eecf3de7e1136d99fffdd781d76bc81a\",\"fileAccess\":\"open\",\"fileType\":\"FASTA\",\"dataType\":\"FASTA\"}]"
            + "}";

    val mapper = new ObjectMapper();
    val expectedSam1Payload = mapper.readValue(sam1PayloadStr, ObjectNode.class);
    val expectedSam2Payload = mapper.readValue(sam2PayloadStr, ObjectNode.class);
    val expected =
        List.of(
            new SubmissionRequest(expectedSam1Payload, "asdf.tsv", STUB_FILE_1),
            new SubmissionRequest(expectedSam2Payload, "asdf.tsv", STUB_FILE_2));

    val submissionBundle = new SubmissionBundle("asdf.tsv", STUB_RECORDS, STUB_FILE_SAMPLE_MAP);

    val fileMapper = new PayloadFileMapper(STUB_PAYLOAD_TEMPLATE);
    val actual = fileMapper.submissionBundleToSubmissionRequests(submissionBundle);

    assertThat(actual).hasSameElementsAs(expected);
  }
}
