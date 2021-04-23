package org.cancogenvirusseq.muse.config.websecurity;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "cors")
public class CorsProperties {
  private List<String> domainPatterns;
  private Long maxAge;
}
