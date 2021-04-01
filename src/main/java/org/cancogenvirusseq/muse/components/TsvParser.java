package org.cancogenvirusseq.muse.components;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

@Slf4j
@RequiredArgsConstructor
public class TsvParser {
  public static Stream<Map<String, String>> parseTsvStrToFlatRecords(String s) {
    val lines = s.split("\n");
    val headers = lines[0].trim().split("\t");

      return Arrays.stream(lines)
          .skip(1)
          .filter(line -> line != null && !line.trim().equals(""))
          .map(
              line -> {
                val data = line.split("\t");
                val dataJson = new HashMap<String, String>();

                for (int i = 0; i < headers.length; ++i) {
                  dataJson.put(headers[i], i > data.length ? "" : data[i]);
                }
                return dataJson;
              });
  }
}
