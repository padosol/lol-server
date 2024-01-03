package com.example.lolserver.match.service;

import com.example.lolserver.riot.RiotAPI;
import com.example.lolserver.summoner.dto.SummonerDto;
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

    public Mono<Map<String, Object>> findMatchBySummonerName(String summonerName) {

        return riotAPI.getWebClient()
                .get()
                .uri("https://kr.api.riotgames.com/lol/summoner/v4/summoners/by-name/" + summonerName)
                .retrieve()
                .bodyToMono(SummonerDto.class)
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
                                    .bodyToMono(new ParameterizedTypeReference<List<String>>() {})
                                    .flatMap(
                                            matchList -> {
                                                System.out.println(matchList);

                                                WebClient webClient = riotAPI.getWebClient();

                                                Map<String, Object> matchMap = new HashMap<>();

                                                Flux<Map<String, Object>> flux = Flux.fromArray(matchList.toArray())
                                                        .flatMap(matchId -> webClient.get()
                                                                .uri("https://asia.api.riotgames.com/lol/match/v5/matches/" + matchId)
                                                                .retrieve()
                                                                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                                                                })
                                                                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                                                                        .filter(throwable -> throwable instanceof WebClientResponseException)
                                                                )
                                                        );


//                                                return Flux.merge(Flux.fromArray(matchList.toArray())
//                                                        .map( matchId -> webClient.get()
//                                                                .uri("https://asia.api.riotgames.com/lol/match/v5/matches/" + matchId)
//                                                                .retrieve()
//                                                                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
//                                                                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
//                                                                        .filter(throwable -> throwable instanceof WebClientResponseException))))
//                                                        .flatMap(
//                                                                result -> {
//                                                                    return Mono.just(result);
//                                                                }
//                                                        );

                                                return Mono.just(matchMap);
                                            }
                                    );

                        }
                );
    }

}
