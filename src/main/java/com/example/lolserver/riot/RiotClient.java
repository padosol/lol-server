package com.example.lolserver.riot;

import com.example.lolserver.riot.dto.account.AccountDto;
import com.example.lolserver.riot.dto.summoner.SummonerDTO;
import com.example.lolserver.web.dto.data.SummonerData;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;


@Component
public class RiotClient {

    private HttpClient client;
    private ObjectMapper objectMapper;

    RiotClient() {
        this.client = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    public AccountDto getAccount(String gameName, String tagLine) throws IOException, InterruptedException {


        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create("https://asia.api.riotgames.com/riot/account/v1/accounts/by-riot-id/"+gameName+"/" + tagLine))
                .headers(headers())
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        AccountDto accountDto = objectMapper.readValue(response.body(), AccountDto.class);

        return accountDto;
    }

    public SummonerDTO getSummoner(String summonerName) throws IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create("https://kr.api.riotgames.com/lol/summoner/v4/summoners/by-name/" + summonerName))
                .headers(headers())
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        SummonerDTO summoner = objectMapper.readValue(response.body(), SummonerDTO.class);

        return summoner;
    }

    public String[] headers() {
        return new String[] {
                "User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                "Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7",
                "Accept-Charset", "application/x-www-form-urlencoded; charset=UTF-8",
                "Origin", "https://developer.riotgames.com",
                "X-Riot-Token", "RGAPI-a01f4988-12c3-4672-b3a7-232ac9327810"
        };
    }

}
