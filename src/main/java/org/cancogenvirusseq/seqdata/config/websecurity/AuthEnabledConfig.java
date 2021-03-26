package org.cancogenvirusseq.seqdata.config.websecurity;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toUnmodifiableList;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.util.Base64;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

@Slf4j
@Profile("secure")
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@RequiredArgsConstructor
public class AuthEnabledConfig {
  private final AuthProperties authProperties;
  private final ResourceLoader resourceLoader;

  @Bean
  public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http) {
    http.csrf()
        .disable()
        .authorizeExchange()
        .pathMatchers("/actuator/**")
        .permitAll()
        // WES-API endpoints
        .pathMatchers("/submit", "/uploads/**", "/download")
        .permitAll()
        .pathMatchers(
            "/v2/api-docs",
            "/configuration/ui",
            "/swagger-resources/**",
            "/configuration/security",
            "/swagger-ui/**",
            "/webjars/**")
        .permitAll()
        .and()
        .authorizeExchange()
        .anyExchange()
        .authenticated()
        .and()
        .oauth2ResourceServer()
        .jwt()
        .jwtDecoder(jwtDecoder())
        .jwtAuthenticationConverter(grantedAuthoritiesExtractor());
    return http.build();
  }

  @Bean
  public Function<Authentication, Boolean> writeScopeChecker() {
    val expectedScopes = authProperties.getScopes().getWrite();
    return new ScopeChecker(expectedScopes);
  }

  @Bean
  public Function<Authentication, Boolean> readScopeChecker() {
    val expectedScopes =
        Lists.newArrayList(
            Iterables.concat(
                authProperties.getScopes().getRead(), authProperties.getScopes().getWrite()));
    return new ScopeChecker(expectedScopes);
  }

  private Converter<Jwt, Mono<AbstractAuthenticationToken>> grantedAuthoritiesExtractor() {
    JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
    jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(
        jwtToGrantedAuthoritiesConverter());
    return new ReactiveJwtAuthenticationConverterAdapter(jwtAuthenticationConverter);
  }

  private Converter<Jwt, Collection<GrantedAuthority>> jwtToGrantedAuthoritiesConverter() {
    return (jwt) -> {
      val scopesBuilder = ImmutableList.<String>builder();

      try {
        val context = (Map<String, Object>) jwt.getClaims().get("context");
        scopesBuilder.addAll((Collection<String>) context.get("scope"));
      } catch (Exception e) {
        log.error("Unable to extract scopes from JWT");
      }

      val scopes = scopesBuilder.build();

      log.debug("JWT scopes: " + scopes);

      return scopes.stream().map(SimpleGrantedAuthority::new).collect(toList());
    };
  }

  @SneakyThrows
  private ReactiveJwtDecoder jwtDecoder() {
    String publicKeyStr;

    val publicKeyUrl = authProperties.getJwtPublicKeyUrl();
    if (publicKeyUrl != null && !publicKeyUrl.isEmpty()) {
      publicKeyStr = fetchJWTPublicKey(publicKeyUrl);
    } else {
      publicKeyStr = authProperties.getJwtPublicKeyStr();
    }

    val publicKeyContent =
        publicKeyStr
            .replaceAll("\\n", "")
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "");

    KeyFactory kf = KeyFactory.getInstance("RSA");

    X509EncodedKeySpec keySpecX509 =
        new X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyContent));
    RSAPublicKey publicKey = (RSAPublicKey) kf.generatePublic(keySpecX509);

    return NimbusReactiveJwtDecoder.withPublicKey(publicKey).build();
  }

  /** Call EGO server for public key to use when verifying JWTs */
  @SneakyThrows
  private String fetchJWTPublicKey(String publicKeyUrl) {
    log.info("Fetching EGO public key");
    val publicKeyResource = resourceLoader.getResource(publicKeyUrl);

    val stringBuilder = new StringBuilder();
    val reader = new BufferedReader(new InputStreamReader(publicKeyResource.getInputStream()));

    reader.lines().forEach(stringBuilder::append);
    return stringBuilder.toString();
  }

  @RequiredArgsConstructor
  static class ScopeChecker implements Function<Authentication, Boolean> {
    private final List<String> expectedScopes;

    @Override
    public Boolean apply(Authentication authentication) {
      val scopes =
          authentication.getAuthorities().stream()
              .map(Objects::toString)
              .collect(toUnmodifiableList());

      val foundScopes =
          scopes.stream().filter(expectedScopes::contains).collect(toUnmodifiableList());

      return foundScopes.size() > 0;
    }
  }
}
