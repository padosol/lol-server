package com.example.lolserver.riot;

import com.example.lolserver.riot.dto.account.AccountDto;
import com.example.lolserver.riot.dto.league.LeagueEntryDTO;
import com.example.lolserver.riot.dto.league.LeagueListDTO;
import com.example.lolserver.riot.dto.match.MatchDto;
import com.example.lolserver.riot.dto.summoner.SummonerDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CountDownLatch;


@Slf4j
@Component
public class RiotClient {

    private final WebClient webClient;
    private final HttpClient client;
    private final ObjectMapper objectMapper;
    private final Long START_TIME = 1704855600L;
    private final String API_KEY;

    private static int retryCount = 0;
    private static int retryTime = 0;

    RiotClient(
            WebClient webClient,
            @Value("${riot.api.key}") String apiKey
               ) {
        this.webClient = webClient;
        this.client = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.API_KEY = apiKey;
    }

    public AccountDto getAccount(String gameName, String tagLine) throws IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create("https://asia.api.riotgames.com/riot/account/v1/accounts/by-riot-id/" +
                        URLEncoder.encode(gameName, StandardCharsets.UTF_8) + "/" + URLEncoder.encode(tagLine, StandardCharsets.UTF_8)))
                .headers(headers())
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        return objectMapper.readValue(response.body(), AccountDto.class);
    }

    public AccountDto getAccountByPuuid(String puuid) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create("https://asia.api.riotgames.com/riot/account/v1/accounts/by-puuid/" + puuid))
                .headers(headers())
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        return objectMapper.readValue(response.body(), AccountDto.class);

    }

    public SummonerDTO getSummoner(String summonerName) throws IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create("https://kr.api.riotgames.com/lol/summoner/v4/summoners/by-name/" + summonerName))
                .headers(headers())
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        return objectMapper.readValue(response.body(), SummonerDTO.class);
    }

    public SummonerDTO getSummonerByPuuid(String puuid) throws IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create("https://kr.api.riotgames.com/lol/summoner/v4/summoners/by-puuid/" + puuid))
                .headers(headers())
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        return objectMapper.readValue(response.body(), SummonerDTO.class);
    }


    public SummonerDTO getSummoner(String pathValue, SummonerPathType type) throws IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create("https://kr.api.riotgames.com/lol/summoner/v4/summoners" + type.type + "/" + pathValue))
                .headers(headers())
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        return objectMapper.readValue(response.body(), SummonerDTO.class);
    }

    public Set<LeagueEntryDTO> getEntries(String summonerId) throws IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create("https://kr.api.riotgames.com/lol/league/v4/entries/by-summoner/" + summonerId))
                .headers(headers())
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        return objectMapper.readValue(response.body(), new TypeReference<Set<LeagueEntryDTO>>() {});
    }

    public LeagueListDTO getLeagues(String leagueId) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create("https://kr.api.riotgames.com/lol/league/v4/leagues/" + leagueId))
                .headers(headers())
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        return objectMapper.readValue(response.body(), LeagueListDTO.class);
    }

    public List<String> getMatchesByPuuid(String puuid, MatchParameters matchParameters) throws IOException, InterruptedException {

        URI https = UriComponentsBuilder.newInstance()
                .scheme("https")
                .host("asia.api.riotgames.com")
                .path("lol/match/v5/matches/by-puuid/" + puuid + "/ids")
                .queryParams(matchParameters.getParams())
                .build().toUri();

        log.debug("MatchesByPuuid Https: {}", https);

        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(https)
                .headers(headers())
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        return Arrays.stream(objectMapper.readValue(response.body(), String[].class)).toList();
    }

    public MatchDto getMatchesByMatchId(String matchId) throws IOException, InterruptedException {

        Long start = System.currentTimeMillis();

        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create("https://asia.api.riotgames.com/lol/match/v5/matches/" + matchId))
                .headers(headers())
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        Long end = System.currentTimeMillis();

        log.info("getMatchesByMatchId: {}ms", end - start);

        return objectMapper.readValue(response.body(), MatchDto.class);
    }

    public List<MatchDto> getMatchesByMatchIds(List<String> matchIds) {

        CountDownLatch latch = new CountDownLatch(matchIds.size());

        List<MatchDto> matchList = Collections.synchronizedList(new ArrayList<MatchDto>());

        List<Mono<MatchDto>> monoList = new ArrayList<>();
        for (String matchId : matchIds) {
            Mono<MatchDto> matchDtoMono = webClient.get()
                    .uri(URI.create("https://asia.api.riotgames.com/lol/match/v5/matches/" + matchId))
                    .retrieve()
                    .bodyToMono(MatchDto.class);

            monoList.add(matchDtoMono);
        }

        Flux.fromIterable(monoList)
                .delayElements(Duration.ofMillis(50))
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

        return matchList;
    }

    public List<String> getAllMatchesByPuuid(String puuid) throws IOException, InterruptedException {
        List<String> matchList = new ArrayList<>();

        boolean flag = true;
        int start = 0;
        int count = 100;
        long endTime = Instant.now().getEpochSecond();
        while(flag) {
            MatchParameters matchParameters = MatchParameters.builder()
                    .startTime(START_TIME)
                    .endTime(endTime)
                    .start(0)
                    .count(100).build();

            URI https = UriComponentsBuilder.newInstance()
                    .scheme("https")
                    .host("asia.api.riotgames.com")
                    .path("lol/match/v5/matches/by-puuid/" + puuid + "/ids")
                    .queryParams(matchParameters.getParams())
                    .build().toUri();

            log.debug("MatchesByPuuid Https: {}", https);

            HttpRequest request = HttpRequest.newBuilder()
                    .GET()
                    .uri(https)
                    .headers(headers())
                    .build();

            log.debug("request 요청: {}", request);

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            List<String> matchIds = Arrays.stream(objectMapper.readValue(response.body(), String[].class)).toList();
            matchList.addAll(matchIds);

            if(matchIds.size() == 100) {
                MatchDto matchDto = getMatchesByMatchId(matchIds.get(99));
                endTime = matchDto.getInfo().getGameCreation() / 1000;
            } else {
                flag = false;
            }

            log.debug("전체 matchList 사이즈: {}", matchList.size());
        }

        return matchList;
    }

    public List<MatchDto> getAllMatchDto(List<String> matchIds) throws IOException, InterruptedException {

        List<MatchDto> matchList = new ArrayList<>();

        for (String matchId : matchIds) {
            MatchDto matchDto = getMatchesByMatchId(matchId);
            matchList.add(matchDto);
        }

        return matchList;
    }


    public String getAPI_KEY() {
        return this.API_KEY;
    }


    public String[] headers() {
        return new String[] {
                "User-Agent", "MMR",
                "Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7",
                "Accept-Charset", "application/x-www-form-urlencoded; charset=UTF-8",
                "X-Riot-Token", API_KEY
        };
    }

}
