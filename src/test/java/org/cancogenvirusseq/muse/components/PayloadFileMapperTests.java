package org.cancogenvirusseq.muse.components;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.SneakyThrows;
import lombok.val;
import org.cancogenvirusseq.muse.model.SubmissionBundle;
import org.cancogenvirusseq.muse.model.SubmissionRequest;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.cancogenvirusseq.muse.components.ComponentTestStubs.*;

public class PayloadFileMapperTests {

  @Test
  @SneakyThrows
  void testPayloadsMappedToFiles() {
    val finalPayload1Str =
        "{ \"samples\": [ {\"submitterSampleId\": \"sam1\"}], "+
          "\"age\":123, " +
          "\"files\":[{\"fileName\":\"sam1.fasta\",\"fileSize\":26,\"fileMd5sum\":\"f433d470a7bacc3bdcdafeb1a4b4d758\",\"fileAccess\":\"open\",\"fileType\":\"FASTA\",\"dataType\":\"FASTA\"}]" +
        "}";

    val mapper = new ObjectMapper();
    val finalPayload1 = mapper.readValue(finalPayload1Str, ObjectNode.class);

    val submissionBundle = new SubmissionBundle(
            "asdf.tsv",
            EXPECTED_TSV_RECORDS,
            sampleIdToFileMeta);

    val fileMapper = new PayloadFileMapper(PAYLOAD_TEMPLATE);
    val source = fileMapper.submissionBundleToSubmissionRequests(submissionBundle);
    val expected = List.of(new SubmissionRequest(finalPayload1, "asdf.tsv", file1Meta));
    assertThat(source).hasSameElementsAs(expected);
  }
}
