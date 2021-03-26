package org.cancogenvirusseq.seqdata.config.websecurity;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@EnableWebFluxSecurity
@Profile("!secure")
public class AuthDisabledConfig {
  @Bean
  public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http) {
    http.csrf().disable().authorizeExchange().pathMatchers("/**").permitAll();
    return http.build();
  }
}
