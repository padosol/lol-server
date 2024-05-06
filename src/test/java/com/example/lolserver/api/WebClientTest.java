package com.example.lolserver.api;

import com.example.lolserver.riot.dto.match.MatchDto;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class WebClientTest {

    @Test
    void 외부_API_호출() {

        Long start = System.currentTimeMillis();

        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("User-Agent", "MMR");
        headers.add("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7");
        headers.add("Accept-Charset", "application/x-www-form-urlencoded; charset=UTF-8");
        headers.add("X-Riot-Token", "RGAPI-e6d2cce3-37b3-4b2a-bb54-3859139142d3");

        List<String> matchIds = List.of(
                "KR_7057643160",
                "KR_7056708028",
                "KR_7056680322",
                "KR_7056636233",
                "KR_7056568933",
                "KR_7056520350",
                "KR_7056423268",
                "KR_7056317939",
                "KR_7056232798",
                "KR_7056164109",
                "KR_7056070817",
                "KR_7056021170",
                "KR_7055925439",
                "KR_7055721106",
                "KR_7055633558",
                "KR_7055439749",
                "KR_7055402306",
                "KR_7055034397",
                "KR_7055002632",
                "KR_7054936788"
        );

        WebClient webClient = WebClient.builder()
                .defaultHeaders(
                        httpHeaders -> httpHeaders.addAll(headers)
                )
                .build();

        List<MatchDto> matchList = Collections.synchronizedList(new ArrayList<MatchDto>());
        CountDownLatch latch = new CountDownLatch(matchIds.size());
        List<Mono<MatchDto>> monoList = new ArrayList<>();

        for (String matchId : matchIds) {
            Mono<MatchDto> matchDtoMono = webClient.get()
                    .uri(URI.create("https://asia.api.riotgames.com/lol/match/v5/matches/" + matchId))
                    .retrieve()
                    .bodyToMono(MatchDto.class);

            monoList.add(matchDtoMono);
        }

        Flux.fromIterable(monoList)
                .subscribe(
                        matchDtoMono -> matchDtoMono.subscribe(
                                matchDto -> {
                                    matchList.add(matchDto);
                                    latch.countDown();
                                    System.out.println(matchDto.getMetadata().getMatchId());
                                    System.out.println(latch.getCount());
                                }
                        )
                );

        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        Long end = System.currentTimeMillis();

        System.out.println(end - start);

        Assertions.assertThat(matchIds.size()).isEqualTo(matchList.size());
    }


}
