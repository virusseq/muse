package org.cancogenvirusseq.seqdata.config.websecurity;

import com.google.common.collect.ImmutableList;

import lombok.Data;
import lombok.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "auth")
public class AuthProperties {
  String jwtPublicKeyUrl;

  String jwtPublicKeyStr;

  Scopes scopes;

  @Value
  @Data
  @ConstructorBinding
  public static class Scopes {
    ImmutableList<String> read;
    ImmutableList<String> write;

    public Scopes(List<String> read, List<String> write) {
      this.read = ImmutableList.copyOf(read);
      this.write = ImmutableList.copyOf(write);
    }
  }
}
