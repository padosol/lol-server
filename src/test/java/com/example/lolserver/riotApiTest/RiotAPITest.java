package com.example.lolserver.riotApiTest;

import com.example.lolserver.summoner.dto.SummonerDto;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.util.function.Consumer;

public class RiotAPITest {


    @Test
    void 유저정보얻기() {

        String userName = "훈상한";

        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        headers.set("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7");
        headers.set("Accept-Charset", "application/x-www-form-urlencoded; charset=UTF-8");
        headers.set("Origin", "https://developer.riotgames.com");
        headers.set("X-Riot-Token", "RGAPI-a01f4988-12c3-4672-b3a7-232ac9327810");

        WebClient webClient = WebClient.builder()
                .baseUrl("https://kr.api.riotgames.com/lol/summoner/v4/summoners/by-name/훈상한")
                .defaultHeaders(httpHeaders -> httpHeaders.addAll(headers)).build();


        Mono<SummonerDto> summonerDtoMono = webClient.get()
                .retrieve()
                .bodyToMono(SummonerDto.class);

        SummonerDto block = summonerDtoMono.block();


//        summonerDtoMono.subscribe(
//                data -> {
//                    System.out.println(data);
//                },
//                error -> {
//                    // Handle any errors that may occur during the API call
//                    System.err.println("Error: " + error.getMessage());
//                },
//                () -> {
//                    // Handle completion (optional)
//                    System.out.println("API call completed");
//                }
//        );


        System.out.println("test");

    }

    @Test
    void 마스터티어유저() {

    }


}

