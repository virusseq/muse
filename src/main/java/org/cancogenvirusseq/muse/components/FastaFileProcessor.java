package org.cancogenvirusseq.muse.components;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.cancogenvirusseq.muse.model.SubmissionFile;
import org.cancogenvirusseq.muse.model.SubmissionUpload;

@Slf4j
public class FastaFileProcessor {
  public static final String FASTA_TYPE = "FASTA";
  public static final String FASTA_FILE_EXTENSION = ".fasta";

  /**
   * Processing submission files into a map of isolateFilename => SubmissionFile
   *
   * @param submissionUpload - submissionUpload to be processed
   * @return concurrent hashmap of isolate filename => SubmissionFile
   */
  public static ConcurrentHashMap<String, SubmissionFile> processFileStrContent(
      SubmissionUpload submissionUpload) {
    log.info("Processing fasta file chunk");

    val isolateToSubmissionFile = new ConcurrentHashMap<String, SubmissionFile>();

    Arrays.stream(submissionUpload.getContent().split("(?=>)"))
        .filter(
            sampleData ->
                sampleData != null && !sampleData.trim().equals("") && sampleData.startsWith(">"))
        .map(String::trim)
        .forEach(
            fc -> {
              val fastaHeaderOpt = extractFastaIsolate(fc);
              if (fastaHeaderOpt.isEmpty()) {
                return;
              }

              val submissionFile =
                  SubmissionFile.builder()
                      .fileExtension(FASTA_FILE_EXTENSION)
                      .fileSize(fc.length())
                      .fileMd5sum(md5(fc).toString())
                      .content(fc)
                      .dataType(FASTA_TYPE)
                      .fileType(FASTA_TYPE)
                      .submittedFileName(submissionUpload.getFilename())
                      .build();

              isolateToSubmissionFile.put(fastaHeaderOpt.get(), submissionFile);
            });

    log.info("Processed fasta file chunk");
    return isolateToSubmissionFile;
  }

  public static Optional<String> extractFastaIsolate(String sampleContent) {
    // get index of first new line
    val fastaHeaderEndNewlineIndex = sampleContent.indexOf("\n");
    if (fastaHeaderEndNewlineIndex == -1) {
      return Optional.empty();
    }

    // isolate is substring from after ">" char to new line (not including)
    return Optional.of(sampleContent.substring(1, fastaHeaderEndNewlineIndex).trim());
  }

  @SneakyThrows
  public static HashCode md5(String input) {
    val hashFunction = Hashing.md5();
    return hashFunction.hashString(input, StandardCharsets.UTF_8);
  }
}
