package com.example.lolserver.common.config;

import com.example.lolserver.common.client.error.ErrorType;
import com.example.lolserver.common.client.error.RestClientException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;

import java.time.Duration;

// 외부 Riot API 호출용 공유 RestClient 베이스. 컨텍스트별 @HttpExchange 프록시 빈은
// 각 컨텍스트의 *.config 에서 이 RestClient 를 주입받아 HttpServiceProxyFactory 로 등록한다.
@Configuration
public class RestClientConfig {

    @Value("${lol.repository.url}")
    public String lolRepositoryUrl;

    private ObjectMapper clientObjectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private ClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(1));
        factory.setReadTimeout(Duration.ofSeconds(5));
        return factory;
    }

    @Bean
    public RestClient restClient() {
        return RestClient.builder()
                .requestFactory(clientHttpRequestFactory())
                .baseUrl(lolRepositoryUrl)
                .messageConverters(converters -> {
                    converters.removeIf(MappingJackson2HttpMessageConverter.class::isInstance);
                    converters.add(new MappingJackson2HttpMessageConverter(clientObjectMapper()));
                })
                .defaultStatusHandler(HttpStatusCode::is4xxClientError, (req, res) -> {
                    throw new RestClientException(ErrorType.EXTERNAL_API_ERROR, "외부 API 요청 에러");
                })
                .build();
    }

}
