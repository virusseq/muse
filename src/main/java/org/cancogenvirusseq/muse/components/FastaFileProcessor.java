package org.cancogenvirusseq.muse.components;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.SneakyThrows;
import lombok.val;
import org.cancogenvirusseq.muse.model.SubmissionFile;

public class FastaFileProcessor {
  public static final String FASTA_TYPE = "FASTA";
  public static final String FASTA_FILE_EXTENSION = ".fasta";

  public static ConcurrentHashMap<String, SubmissionFile> processFileStrContent(String fastaFile) {
    val sampleIdSubmissionFileMap = new ConcurrentHashMap<String, SubmissionFile>();

    Arrays.stream(fastaFile.split("(?=>)"))
        .parallel()
        .filter(sampleData -> sampleData != null && !sampleData.trim().equals(""))
        .forEach(
            fc -> {
              val sampleIdOpt = extractSampleId(fc);
              if (sampleIdOpt.isEmpty()) return;

              sampleIdSubmissionFileMap.put(
                  sampleIdOpt.get(),
                  SubmissionFile.builder()
                      .fileName(sampleIdOpt.get() + FASTA_FILE_EXTENSION)
                      .fileSize(fc.length())
                      .fileMd5sum(md5(fc).toString())
                      .content(fc)
                      .dataType(FASTA_TYPE)
                      .fileType(FASTA_TYPE)
                      .build());
            });

    return sampleIdSubmissionFileMap;
  }

  public static Optional<String> extractSampleId(String sampleContent) {
    // add one to index of delimiter char to get start of sampleId
    val sampleIdStartIndex = sampleContent.indexOf("/") + 1;
    if (sampleIdStartIndex == 0) {
      return Optional.empty();
    }

    val sampleIdEndIndex = sampleContent.indexOf("/", sampleIdStartIndex);
    if (sampleIdEndIndex > sampleContent.length()) {
      return Optional.empty();
    }

    return Optional.of(sampleContent.substring(sampleIdStartIndex, sampleIdEndIndex));
  }

  @SneakyThrows
  public static HashCode md5(String input) {
    val hashFunction = Hashing.md5();
    return hashFunction.hashString(input, StandardCharsets.UTF_8);
  }
}
