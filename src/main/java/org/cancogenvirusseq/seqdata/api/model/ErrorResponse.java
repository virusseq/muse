package org.cancogenvirusseq.seqdata.api.model;

import io.swagger.annotations.ApiModel;
import lombok.*;

@ApiModel(description = "An object that can optionally include information about the error.")
@Data
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ErrorResponse {
  private String msg;
  private Integer statusCode;
}
