package com.example.lolserver.config;

import com.example.lolserver.riot.client.summoner.ChampionRotateRestClient;
import com.example.lolserver.riot.client.summoner.SummonerRestClient;
import com.example.lolserver.support.error.CoreException;
import com.example.lolserver.support.error.ErrorType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class RestClientConfig {

    @Value("${lol.repository.url}")
    public String lolRepositoryUrl;

    @Bean
    public RestClient restClient() {
        return RestClient.builder()
                .baseUrl(lolRepositoryUrl)
                .defaultStatusHandler(HttpStatusCode::is4xxClientError, (req, res) -> {
                    throw new CoreException(ErrorType.EXTERNAL_API_ERROR, "외부 API 요청 에러");
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

}
