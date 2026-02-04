package com.example.lolserver.config;

import com.example.lolserver.restclient.spectator.SpectatorRestClient;
import com.example.lolserver.restclient.summoner.ChampionRotateRestClient;
import com.example.lolserver.restclient.summoner.SummonerRestClient;
import com.example.lolserver.error.ErrorType;
import com.example.lolserver.error.RestClientException;
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
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

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
        factory.setReadTimeout(Duration.ofSeconds(3));
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

    @Bean
    public SummonerRestClient summonerRestClient() {
        RestClientAdapter restClientAdapter = RestClientAdapter
                .create(restClient());

        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(restClientAdapter)
                .build();

        return factory.createClient(SummonerRestClient.class);
    }

    @Bean
    public ChampionRotateRestClient championRotateRestClient() {
        RestClientAdapter restClientAdapter = RestClientAdapter
                .create(restClient());

        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(restClientAdapter)
                .build();

        return factory.createClient(ChampionRotateRestClient.class);
    }

    @Bean
    public SpectatorRestClient spectatorRestClient() {
        RestClientAdapter restClientAdapter = RestClientAdapter
                .create(restClient());

        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(restClientAdapter)
                .build();

        return factory.createClient(SpectatorRestClient.class);
    }

}
