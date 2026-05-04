package com.example.lolserver.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bigquery")
public record BigQueryProperties(
        String projectId,
        String dataset,
        String credentialsLocation
) {
}
