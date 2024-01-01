package com.example.lolserver.riot;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class RiotAPI {

    private static final String apiKey = "RGAPI-a01f4988-12c3-4672-b3a7-232ac9327810";
    private WebClient webClient;

    public RiotAPI() {

        HttpHeaders headers = new HttpHeaders();

        headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        headers.set("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7");
        headers.set("Accept-Charset", "application/x-www-form-urlencoded; charset=UTF-8");
        headers.set("Origin", "https://developer.riotgames.com");
        headers.set("X-Riot-Token", apiKey);

        this.webClient = WebClient.builder()
                .defaultHeaders(httpHeaders -> httpHeaders.addAll(headers))
                .build();
    }

    public WebClient getWebClient(){
        return this.webClient;
    }

}
