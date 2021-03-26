package org.cancogenvirusseq.seqdata.config;

import static java.util.Collections.singletonList;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.*;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.paths.DefaultPathProvider;
import springfox.documentation.spring.web.plugins.Docket;

@Configuration
public class SwaggerConfig {
  @Value("${spring.application.name}")
  private String appName;

  @Value("${spring.application.description}")
  private String appDescription;

  @Value("${spring.application.version}")
  private String appVersion;

  ApiInfo apiInfo() {
    return new ApiInfoBuilder()
        .title(appName)
        .description(appDescription)
        .version(appVersion)
        .build();
  }

  @Bean
  public Docket api(SwaggerProperties properties) {
    return new Docket(DocumentationType.SWAGGER_2)
        .securityContexts(securityContexts())
        .securitySchemes(securitySchemes())
        .select()
        .apis(RequestHandlerSelectors.basePackage("org.cancogenvirusseq.seqdata.api"))
        .build()
        .host(properties.host)
        .pathProvider(
            new DefaultPathProvider() {
              @Override
              public String getDocumentationPath() {
                return properties.getBaseUrl();
              }
            })
        .apiInfo(apiInfo());
  }

  private List<SecurityScheme> securitySchemes() {
    return singletonList(new ApiKey("JWT", "Authorization", "header"));
  }

  private List<SecurityContext> securityContexts() {
    AuthorizationScope[] authorizationScopes = {
      new AuthorizationScope("global", "accessEverything")
    };
    val securityRefs = singletonList(new SecurityReference("JWT", authorizationScopes));
    return List.of(SecurityContext.builder().securityReferences(securityRefs).build());
  }

  @Component
  @ConfigurationProperties(prefix = "swagger")
  class SwaggerProperties {
    /** Specify host if application is running behind proxy. */
    @Setter @Getter private String host = "";

    /**
     * If there is url write rule, you may want to set this variable. This value requires host to be
     * not empty.
     */
    @Setter @Getter private String baseUrl = "";
  }
}
