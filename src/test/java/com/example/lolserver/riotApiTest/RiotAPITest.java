package com.example.lolserver.riotApiTest;

import com.example.lolserver.exception.RateLimitException;
import com.example.lolserver.summoner.dto.SummonerDto;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.net.URLEncoder;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class RiotAPITest {


    @Test
    void 유저정보얻기() {

        long startTime = System.currentTimeMillis();

        String userName = "훈상한";

        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        headers.set("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7");
        headers.set("Accept-Charset", "application/x-www-form-urlencoded; charset=UTF-8");
        headers.set("Origin", "https://developer.riotgames.com");
        headers.set("X-Riot-Token", "RGAPI-a01f4988-12c3-4672-b3a7-232ac9327810");

        WebClient webClient = WebClient.builder()
                .baseUrl("https://kr.api.riotgames.com/lol/summoner/v4/summoners/by-name/훈상한")
                .defaultHeaders(httpHeaders -> httpHeaders.addAll(headers))
                .build();


        SummonerDto summonerDto = webClient.get()
                .retrieve()
                .bodyToMono(SummonerDto.class)
                .block();

        WebClient webClient1 = WebClient.builder()
                .baseUrl("https://asia.api.riotgames.com/lol/match/v5/matches/by-puuid/"+summonerDto.getPuuid()+"/ids")
                .defaultHeaders(httpHeaders -> httpHeaders.addAll(headers))
                .build();

        List<String> matchList = webClient1.get()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("start", 0)
                        .queryParam("count", 20)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<String>>() {
                })
                .block();

        List<Map<String, Object>> dataMap = new ArrayList<>();

        // match
        WebClient webClient2 = WebClient.builder()
                .defaultHeaders(httpHeaders -> httpHeaders.addAll(headers))
                .build();

        for(String match : matchList) {
            webClient2.get()
                    .uri("https://asia.api.riotgames.com/lol/match/v5/matches/" + match)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                            .filter(throwable -> throwable instanceof WebClientResponseException))
                    .subscribe(
                            response -> {
                                dataMap.add(response);
                            }
                    );
        }

        try{
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        long endTime = System.currentTimeMillis();

        System.out.println(endTime - startTime + " ms");

    }

    @Test
    void 마스터티어유저() {

    }


}

