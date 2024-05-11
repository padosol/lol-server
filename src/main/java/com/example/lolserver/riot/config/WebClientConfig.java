package com.example.lolserver.riot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    private final String API_KEY;

    public WebClientConfig(
            @Value("${riot.api.key}") String apiKey
    ) {
        this.API_KEY = apiKey;
    }

    @Bean
    public WebClient webCLient() {
        return  WebClient.builder()
                .defaultHeaders(httpHeaders ->
                        httpHeaders.addAll(headers()))
                .build();
    }

    public MultiValueMap<String, String> headers() {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("User-Agent", "MMRTR");
        headers.add("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7");
        headers.add("Accept-Charset", "application/x-www-form-urlencoded; charset=UTF-8");
        headers.add("X-Riot-Token", API_KEY);

        return headers;
    }

}
