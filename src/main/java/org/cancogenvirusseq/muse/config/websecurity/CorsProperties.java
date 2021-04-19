package org.cancogenvirusseq.muse.config.websecurity;

import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "cors")
public class CorsProperties {
  private List<String> domainPatterns;
  private Long maxAge;
}
