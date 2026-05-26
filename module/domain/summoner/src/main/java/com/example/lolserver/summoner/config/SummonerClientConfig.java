package com.example.lolserver.summoner.config;

import com.example.lolserver.summoner.adapter.out.client.restclient.spectator.SpectatorRestClient;
import com.example.lolserver.summoner.adapter.out.client.restclient.summoner.SummonerRestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class SummonerClientConfig {

    private final RestClient restClient;

    public SummonerClientConfig(RestClient restClient) {
        this.restClient = restClient;
    }

    @Bean
    public SummonerRestClient summonerRestClient() {
        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
                .build()
                .createClient(SummonerRestClient.class);
    }

    @Bean
    public SpectatorRestClient spectatorRestClient() {
        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
                .build()
                .createClient(SpectatorRestClient.class);
    }
}
