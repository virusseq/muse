package org.cancogenvirusseq.muse.model.song_score;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ErrorResponse {
  public static final String ERROR_ID_SCHEMA_VIOLATION = "schema.violation";
  public static final String ERROR_ID_ANALYSIS_ID_NOT_FOUND = "analysis.id.not.found";

  String errorId;
  String message;
//  HttpStatus status;
}
