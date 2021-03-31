package org.cancogenvirusseq.muse.components;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import lombok.SneakyThrows;
import lombok.val;
import org.cancogenvirusseq.muse.model.SubmissionFile;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

public class FastaFileProcessor {
  public static final String FASTA_TYPE = "FASTA";
  public static final String FASTA_FILE_EXTENSION = ".fasta";

  public static ConcurrentHashMap<String, SubmissionFile> processFileStrContent(String fastaFile) {

    final ConcurrentHashMap<String, SubmissionFile> repo = new ConcurrentHashMap<>();

    Arrays.stream(fastaFile.split("(?=>)"))
        .parallel()
        .forEach(
            fc -> {
              if (fc == null || fc.trim().equals("")) return;
              val sampleId = fc.split("/")[1];
              if (sampleId == null) return;

              val fileName = sampleId + FASTA_FILE_EXTENSION;

              val submissionFile =
                  SubmissionFile.builder()
                      .fileName(fileName)
                      .fileSize(fc.length())
                      .fileMd5sum(md5(fc).toString())
                      .content(fc)
                      .dataType(FASTA_TYPE)
                      .fileType(FASTA_TYPE)
                      .dataCategory(FASTA_TYPE)
                      .build();
              repo.put(sampleId, submissionFile);
            });

    return repo;
  }

  @SneakyThrows
  public static HashCode md5(String input) {
    val hashFunction = Hashing.md5();
    return hashFunction.hashString(input, StandardCharsets.UTF_8);
  }
}
