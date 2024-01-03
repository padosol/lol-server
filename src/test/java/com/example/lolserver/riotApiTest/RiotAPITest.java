package com.example.lolserver.riotApiTest;

import com.example.lolserver.exception.RateLimitException;
import com.example.lolserver.summoner.dto.SummonerDto;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.*;

public class RiotAPITest {


    @Test
    void 유저정보와매치리스트얻기() {

        long startTime = System.currentTimeMillis();

        String userName = "건동김";

        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        headers.set("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7");
        headers.set("Accept-Charset", "application/x-www-form-urlencoded; charset=UTF-8");
        headers.set("Origin", "https://developer.riotgames.com");
        headers.set("X-Riot-Token", "RGAPI-a01f4988-12c3-4672-b3a7-232ac9327810");

        WebClient webClient = WebClient.builder()
                .baseUrl("https://kr.api.riotgames.com/lol/summoner/v4/summoners/by-name/" + userName)
                .defaultHeaders(httpHeaders -> httpHeaders.addAll(headers))
                .build();

        SummonerDto summonerDto = webClient.get()
                .retrieve()
                .bodyToMono(SummonerDto.class)
                .block();

        /// 유저 정보 얻기
        WebClient summonerInfo = WebClient.builder()
                .baseUrl("https://kr.api.riotgames.com/lol/league/v4/entries/by-summoner/" + summonerDto.getId())
                .defaultHeaders(httpHeaders -> httpHeaders.addAll(headers))
                .build();

        Mono<Set<Map<String, Object>>> setMono = summonerInfo.get()
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Set<Map<String, Object>>>() {
                });

        /// 유저정보 얻기 끝

        // 매치 리스트 얻기
        WebClient webClient1 = WebClient.builder()
                .baseUrl("https://asia.api.riotgames.com/lol/match/v5/matches/by-puuid/"+summonerDto.getPuuid()+"/ids")
                .defaultHeaders(httpHeaders -> httpHeaders.addAll(headers))
                .build();

        Mono<List<String>> listMono = webClient1.get()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("start", 0)
                        .queryParam("count", 20)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<String>>() {});

        // match 리스트 얻기 끝

        Tuple2<Set<Map<String, Object>>, List<String>> block = Mono.zip(setMono, listMono)
                .doOnSuccess(tuple -> {

                    Set<Map<String, Object>> t1 = tuple.getT1();
                    List<String> t2 = tuple.getT2();

                    System.out.println(t1);
                    System.out.println(t2);
                })
                .block();


        System.out.println("test");

        List<String> matchList = block.getT2();

        WebClient matchClient = WebClient.builder()
                .defaultHeaders(httpHeaders -> httpHeaders.addAll(headers))
                .build();

        List<Map<String, Object>> dataMap = new ArrayList<>();

        List<Mono<Map<String, Object>>> monoList = new ArrayList<>();

        for(String matchId : matchList) {
            Mono<Map<String, Object>> mapMono = matchClient.get()
                    .uri("https://asia.api.riotgames.com/lol/match/v5/matches/" + matchId)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                    })
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                            .filter(throwable -> throwable instanceof WebClientResponseException)
                    );

            monoList.add(mapMono);
        }

        List<Map<String, Object>> block1 = Flux.merge(monoList)
                .collectList()
                .map(
                        result -> {
                            return result;
                        }
                ).block();

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        long endTime = System.currentTimeMillis();

        System.out.println(endTime - startTime + " ms");

    }




}

