package com.example.lolserver.gamedata.config;

import com.example.lolserver.gamedata.adapter.out.client.ChampionRotateRestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class ChampionClientConfig {
    private final RestClient restClient;

    public ChampionClientConfig(RestClient restClient) {
        this.restClient = restClient;
    }

    @Bean
    public ChampionRotateRestClient championRotateRestClient() {
        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient)).build()
                .createClient(ChampionRotateRestClient.class);
    }
}
