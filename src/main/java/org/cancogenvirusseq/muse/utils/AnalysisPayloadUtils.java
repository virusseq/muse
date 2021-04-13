package org.cancogenvirusseq.muse.utils;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.experimental.UtilityClass;

/**
 * Collection utility functions for analysis payloads. These function assume SONG analysis base
 * schema form only.
 */
@UtilityClass
public class AnalysisPayloadUtils {
  public static String getFirstSubmitterSampleId(JsonNode analysisJson) {
    return analysisJson.get("samples").get(0).get("submitterSampleId").asText();
  }

  public static String getStudyId(JsonNode analysisJson) {
    return analysisJson.get("studyId").asText();
  }
}
