package com.example.lolserver.match.service;

import com.example.lolserver.match.dto.metadata.MatchDto;
import com.example.lolserver.riot.RiotAPI;
import com.example.lolserver.summoner.dto.SummonerDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MatchService {

    private final RiotAPI riotAPI;
    public Mono<List<MatchDto>> findMatchBySummonerName(String summonerName) {

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
                                    .bodyToMono(List.class).log()
                                    .flatMap(
                                            matchList -> {

                                                WebClient webClient = riotAPI.getWebClient();

                                                Map<String, Object> matchMap = new HashMap<>();

                                                List<Mono<MatchDto>> summonerMatchList = new ArrayList<>();

                                                for(Object matchId : matchList) {

                                                    Mono<MatchDto> matchDto = webClient.get()
                                                            .uri("https://asia.api.riotgames.com/lol/match/v5/matches/" + matchId)
                                                            .retrieve()
                                                            .bodyToMono(MatchDto.class)
                                                            .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                                                                .filter(throwable -> throwable instanceof WebClientResponseException)
                                                            );

                                                    summonerMatchList.add(matchDto);

                                                }

                                                return Flux.fromIterable(summonerMatchList).log()
                                                        .delaySubscription(Duration.ofSeconds(1)).log()
                                                        .flatMap(result -> {

                                                            return result.flatMap(
                                                                    matchData -> {

                                                                        List<String> participants = matchData.getMetadata().getParticipants();

                                                                        int index = 0;

                                                                        for(int i=0;i< participants.size();i++) {
                                                                            if(puuid.equals(participants.get(i))) {
                                                                                index = i;
                                                                                break;
                                                                            }
                                                                        }

                                                                        matchData.setMyIndex(index);

                                                                        return Mono.just(matchData);
                                                                    }
                                                            );

                                                        })
                                                        .collectSortedList((o1, o2) -> {

                                                            Date d1 = new Date(o1.getInfo().getGameCreation());
                                                            Date d2 = new Date(o2.getInfo().getGameCreation());

                                                            return d2.compareTo(d1);
                                                        });

                                            }
                                    );

                        }
                );
    }

}
