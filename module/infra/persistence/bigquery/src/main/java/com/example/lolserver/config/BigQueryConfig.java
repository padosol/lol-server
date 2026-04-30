package com.example.lolserver.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;

@Configuration
@ConditionalOnProperty(name = "stats.datasource", havingValue = "bigquery")
@EnableConfigurationProperties(BigQueryProperties.class)
public class BigQueryConfig {

    @Bean
    public BigQuery bigQuery(BigQueryProperties properties) throws IOException {
        BigQueryOptions.Builder builder = BigQueryOptions.newBuilder();

        if (StringUtils.hasText(properties.projectId())) {
            builder.setProjectId(properties.projectId());
        }

        if (StringUtils.hasText(properties.credentialsLocation())) {
            Resource resource = new DefaultResourceLoader().getResource(properties.credentialsLocation());
            try (InputStream in = resource.getInputStream()) {
                builder.setCredentials(GoogleCredentials.fromStream(in));
            }
        }

        return builder.build().getService();
    }
}
