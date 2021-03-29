package org.cancogenvirusseq.seqdata.components;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import lombok.SneakyThrows;
import lombok.val;
import org.cancogenvirusseq.seqdata.model.FileMeta;

public class FileProcessor {
  public static final String FASTA_TYPE = "FASTA";
  public static final String FASTA_FILE_EXTENSION = ".v0.fasta";

  public static ConcurrentHashMap<String, FileMeta> processFileStrContent(String fileStrContent) {
  final ConcurrentHashMap<String, FileMeta> repo = new ConcurrentHashMap<>();
    Arrays.stream(fileStrContent.split(">"))
        .parallel()
        .forEach(
            fc -> {
              if (fc == null || fc.trim().equals("")) return;
              val sampleId = fc.split("/")[1];
              if (sampleId == null) return;

              // TODO - strings are immutable and this will duplicating fc, not append!
              val fBadFix = ">" + fc;

              val fileName = sampleId + FASTA_FILE_EXTENSION;

              try {
                FileWriter myWriter = new FileWriter(fileName);
                myWriter.write(fBadFix);
                myWriter.close();
                System.out.println("Successfully wrote to the file.");
              } catch (IOException e) {
                System.out.println("An error occurred.");
                e.printStackTrace();
                return;
              }

              val fileMeta =
                  FileMeta.builder()
                      .fileMd5sum(md5(fBadFix).toString())
                      .fileName(fileName)
                      .fileSize(fBadFix.length())
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
