package com.example.lolserver.match.service;

import com.example.lolserver.match.dto.MatchDto;
import com.example.lolserver.riot.RiotAPI;
import com.example.lolserver.summoner.dto.SummonerDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MatchService {

    private final RiotAPI riotAPI;
    public Flux<Map<String, Object>> findMatchBySummonerName(String summonerName) {

        return riotAPI.getWebClient()
                .get()
                .uri("https://kr.api.riotgames.com/lol/summoner/v4/summoners/by-name/" + summonerName)
                .retrieve()
                .bodyToFlux(SummonerDto.class)
                .flatMap(
                        response -> {

                            String puuid = response.getPuuid();

                            return riotAPI.getWebClient()
                                    .get()
                                    .uri(uriBuilder -> uriBuilder
                                            .scheme("https")
                                            .host("asia.api.riotgames.com")
                                            .path("/lol/match/v5/matches/by-puuid/"+puuid+"/ids")
                                            .queryParam("start", 0)
                                            .queryParam("count", 20)
                                            .build())
                                    .retrieve()
                                    .bodyToFlux(String[].class)
                                    .flatMap(
                                            matchList -> {
                                                System.out.println(matchList);

                                                WebClient webClient = riotAPI.getWebClient();

                                                Map<String, Object> matchMap = new HashMap<>();

                                                return Flux.just(matchMap);

//                                                return Flux.fromArray(matchList.getMatchIds().toArray())
//                                                        .flatMap(matchId -> webClient.get()
//                                                                .uri("https://asia.api.riotgames.com/lol/match/v5/matches/" + matchId)
//                                                                .retrieve()
//                                                                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
//                                                                })
//                                                                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
//                                                                        .filter(throwable -> throwable instanceof WebClientResponseException)
//                                                                )
//                                                        ).flatMap(Flux::just);
//
////                                                return Flux.merge(Flux.fromArray(matchList.toArray())
////                                                        .map( matchId -> webClient.get()
////                                                                .uri("https://asia.api.riotgames.com/lol/match/v5/matches/" + matchId)
////                                                                .retrieve()
////                                                                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
////                                                                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
////                                                                        .filter(throwable -> throwable instanceof WebClientResponseException))))
////                                                        .flatMap(
////                                                                result -> {
////                                                                    return Mono.just(result);
////                                                                }
////                                                        );
                                            }
                                    );

                        }
                );
    }

}
