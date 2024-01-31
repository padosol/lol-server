package com.example.lolserver.web.service;

import com.example.lolserver.entity.match.Match;
import com.example.lolserver.entity.match.MatchSummoner;
import com.example.lolserver.entity.summoner.Summoner;
import com.example.lolserver.riot.RiotAPI;
import com.example.lolserver.riot.dto.league.LeagueEntryDTO;
import com.example.lolserver.riot.dto.match.MatchDto;
import com.example.lolserver.riot.dto.match.ParticipantDto;
import com.example.lolserver.riot.dto.summoner.SummonerDTO;
import com.example.lolserver.web.repository.SummonerRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class LolServiceImpl implements LolService{

    private final SummonerRepository summonerRepository;
    private final ObjectMapper objectMapper;
    private final RiotAPI riotAPI;

    @Override
    public void findSummoner(String summonerName) throws IOException, InterruptedException {

        Optional<Summoner> summonerByName = summonerRepository.findSummonerByName(summonerName);

        if(summonerByName.isEmpty()) {
            // 데이터베이스에 해당 유저가 없으면 api 요청해서 정보를 가져옴
            // 유저 검색 -> matchList 검색 -> matchList 상세 검색 -> db에 데이터넣기
            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create("https://kr.api.riotgames.com/lol/summoner/v4/summoners/by-name/" + summonerName))
                    .headers(riotAPI.headers())
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            SummonerDTO summonerDTO = objectMapper.readValue(response.body(), SummonerDTO.class);

            Summoner summoner = summonerRepository.save(summonerDTO.toEntity());

            request = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create("https://kr.api.riotgames.com/lol/league/v4/entries/by-summoner/" + summoner.getId()))
                    .headers()
                    .build();

            HttpResponse<String> leagueResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
            Set<LeagueEntryDTO> leagueEntryDTOS = objectMapper.readValue(leagueResponse.body(), new TypeReference<Set<LeagueEntryDTO>>() {});

            for (LeagueEntryDTO leagueEntryDTO : leagueEntryDTOS) {
                
            }

            request = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create("https://asia.api.riotgames.com/lol/match/v5/matches/by-puuid/"+summoner.getPuuid()+"/ids?start=0&count=20"))
                    .headers(riotAPI.headers())
                    .build();

            HttpResponse<String> matchListResponse = client.send(request, HttpResponse.BodyHandlers.ofString());

            List<String> matchList = Arrays.asList(objectMapper.readValue(matchListResponse.body(), String[].class));

            for (String matchId : matchList) {

                request = HttpRequest.newBuilder()
                        .GET()
                        .uri(URI.create("https://asia.api.riotgames.com/lol/match/v5/matches/" + matchId))
                        .headers(riotAPI.headers())
                        .build();

                HttpResponse<String> matchResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
                MatchDto matchDTO = objectMapper.readValue(matchResponse.body(), MatchDto.class);

                Match match = matchDTO.toEntity();

                List<ParticipantDto> participants = matchDTO.getInfo().getParticipants();

                for (ParticipantDto participant : participants) {
                    MatchSummoner matchSummoner = participant.toEntity(match);

                }


            }

            


        } else {


            
        }


    }
}
