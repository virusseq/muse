package org.cancogenvirusseq.muse.config.db;

import lombok.Data;
import lombok.NonNull;
import lombok.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "postgres")
public class PostgresProperties {
    private String host;
    private int port;
    private String database;
    private String username;
    private String password;
}
