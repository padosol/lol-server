package com.example.lolserver.summoner.service;

import com.example.lolserver.riot.RiotAPI;
import com.example.lolserver.summoner.dto.SummonerDto;
import com.example.lolserver.summoner.entiry.Summoner;
import com.example.lolserver.summoner.repository.SummonerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.yaml.snakeyaml.util.UriEncoder;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SummonerService {

    private final SummonerRepository summonerRepository;
    private final RiotAPI riotAPI;

    public Mono<Set<Map<String, Object>>> findSummonerByName(String name) {

        return riotAPI.getWebClient()
                .get()
                .uri("https://kr.api.riotgames.com/lol/summoner/v4/summoners/by-name/" + UriEncoder.encode(name))
                .retrieve()
                .bodyToMono(SummonerDto.class)
                .flatMap(
                        response -> {
                            return riotAPI.getWebClient().get()
                                    .uri("https://kr.api.riotgames.com/lol/league/v4/entries/by-summoner/" + response.getId())
                                    .retrieve()
                                    .bodyToMono(new ParameterizedTypeReference<Set<Map<String, Object>>>() {})
                                    .flatMap(
                                            result -> {
                                                return Mono.just(result);
                                            }
                                    );
                        }
                );

    }

}
