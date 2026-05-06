package com.example.lolserver.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
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

        String credentialsSource;
        if (StringUtils.hasText(properties.credentialsLocation())) {
            Resource resource = new DefaultResourceLoader().getResource(properties.credentialsLocation());
            try (InputStream in = resource.getInputStream()) {
                builder.setCredentials(GoogleCredentials.fromStream(in));
            }
            credentialsSource = properties.credentialsLocation();
        } else {
            credentialsSource = "ADC (GOOGLE_APPLICATION_CREDENTIALS or workload identity)";
        }

        log.info("[stats] BigQuery 데이터소스 활성화 - projectId={}, dataset={}, credentials={}",
                properties.projectId(), properties.dataset(), credentialsSource);

        return builder.build().getService();
    }
}
