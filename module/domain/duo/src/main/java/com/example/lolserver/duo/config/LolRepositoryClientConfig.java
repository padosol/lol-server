package com.example.lolserver.duo.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class LolRepositoryClientConfig {

    @Bean
    public RestClient lolRepositoryRestClient(LolRepositoryClientProperties properties) {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(properties.getConnectTimeout())
                .withReadTimeout(properties.getReadTimeout());

        return RestClient.builder()
                .requestFactory(ClientHttpRequestFactories.get(settings))
                .baseUrl(properties.getInternalUrl())
                .build();
    }

    @Bean
    public CircuitBreaker lolRepositoryCircuitBreaker(LolRepositoryClientProperties properties) {
        LolRepositoryClientProperties.CircuitBreaker circuitBreaker = properties.getCircuitBreaker();
        return CircuitBreaker.of("lol-repository", CircuitBreakerConfig.custom()
                .slidingWindowSize(circuitBreaker.getSlidingWindowSize())
                .minimumNumberOfCalls(circuitBreaker.getMinimumNumberOfCalls())
                .failureRateThreshold(circuitBreaker.getFailureRateThreshold())
                .waitDurationInOpenState(circuitBreaker.getWaitDurationInOpenState())
                .build());
    }
}
