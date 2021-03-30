package org.cancogenvirusseq.muse.components;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import lombok.SneakyThrows;
import lombok.val;
import org.cancogenvirusseq.muse.model.SubmissionFile;

public class FileProcessor {
  public static final String FASTA_TYPE = "FASTA";
  public static final String FASTA_FILE_EXTENSION = ".v0.fasta";

  public static ConcurrentHashMap<String, SubmissionFile> processFileStrContent(
      String dir, String fileStrContent) {
    File file = new File(dir);
    boolean dirCreated = file.mkdir();
    if (!dirCreated) {
      throw new Error("Failed to create dir!");
    }

    final ConcurrentHashMap<String, SubmissionFile> repo = new ConcurrentHashMap<>();

    Arrays.stream(fileStrContent.split("(?=>)"))
        .parallel()
        .forEach(
            fc -> {
              if (fc == null || fc.trim().equals("")) return;
              val sampleId = fc.split("/")[1];
              if (sampleId == null) return;

              val fileName = sampleId + FASTA_FILE_EXTENSION;
              val filePath = dir + "/" + fileName;

              try {
                FileWriter myWriter = new FileWriter(filePath);
                myWriter.write(fc);
                myWriter.close();
                System.out.println("Successfully wrote to the file.");
              } catch (IOException e) {
                System.out.println("An error occurred: Failed to write - " + filePath);
                e.printStackTrace();
                return;
              }

              val fileMeta =
                  SubmissionFile.builder()
                      .fileMd5sum(md5(fc).toString())
                      .fileName(fileName)
                      .fileSize(fc.length())
                      // TBD, constants for now
                      .fileType(FASTA_TYPE)
                      .dataCategory(FASTA_TYPE)
                      .dataType(FASTA_TYPE)
                      .build();
              repo.put(sampleId, fileMeta);
            });
    return repo;
  }

  @SneakyThrows
  public static HashCode md5(String input) {
    val hashFunction = Hashing.md5();
    return hashFunction.hashString(input, StandardCharsets.UTF_8);
  }
}
