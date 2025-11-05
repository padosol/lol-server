package com.example.lolserver.config;

import com.example.lolserver.web.dto.LOLResponse;
import com.example.lolserver.web.summoner.client.SummonerRestClient;
import com.example.lolserver.web.summoner.domain.Summoner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class RestClientConfig {
    @Value("${lol.repository.url}")
    public String lolRepositoryUrl;

    @Bean
    public RestClient restClient() {
        return RestClient.builder()
                .baseUrl(lolRepositoryUrl)
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


}
