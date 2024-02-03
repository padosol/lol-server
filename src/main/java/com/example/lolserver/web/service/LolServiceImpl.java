package com.example.lolserver.web.service;

import com.example.lolserver.entity.league.League;
import com.example.lolserver.entity.league.LeagueSummoner;
import com.example.lolserver.entity.match.Match;
import com.example.lolserver.entity.match.MatchSummoner;
import com.example.lolserver.entity.match.MatchTeam;
import com.example.lolserver.entity.match.MatchTeamBan;
import com.example.lolserver.entity.summoner.Summoner;
import com.example.lolserver.riot.RiotClient;
import com.example.lolserver.riot.dto.account.AccountDto;
import com.example.lolserver.riot.dto.league.LeagueEntryDTO;
import com.example.lolserver.riot.dto.league.LeagueListDTO;
import com.example.lolserver.riot.dto.match.BanDto;
import com.example.lolserver.riot.dto.match.MatchDto;
import com.example.lolserver.riot.dto.match.ParticipantDto;
import com.example.lolserver.riot.dto.match.TeamDto;
import com.example.lolserver.riot.dto.summoner.SummonerDTO;
import com.example.lolserver.web.dto.SearchData;
import com.example.lolserver.web.dto.data.SummonerData;
import com.example.lolserver.web.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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

    private final LeagueSummonerRepository leagueSummonerRepository;
    private final MatchSummonerRepository matchSummonerRepository;
    private final MatchTeamBanRepository matchTeamBanRepository;
    private final MatchTeamRepository matchTeamRepository;
    private final SummonerRepository summonerRepository;
    private final LeagueRepository leagueRepository;
    private final MatchRepository matchRepository;
    private final ObjectMapper objectMapper;
    private final RiotClient riotClient;

    @Override
    @Transactional
    public SearchData findSummoner(String summonerName) throws IOException, InterruptedException {

        int index = summonerName.lastIndexOf('#');

        String gameName = "";
        String tagLine = "";

        if(index > -1) {
            gameName = summonerName.substring(0, index);
            tagLine = summonerName.substring(index+1);
        } else {
            gameName = summonerName;
        }

        if(StringUtils.hasText(tagLine)) {
            // 태그라인이 있을 경우 db 정보 조회
            Optional<Summoner> summoner = summonerRepository.findSummonerByGameNameAndTagLine(gameName, tagLine);

            if(summoner.isEmpty()) {
                // 비어 있으면 api 조회
                AccountDto account = riotClient.getAccount(gameName, tagLine);

                if(account.isError()) {
                    return new SearchData(true);
                }

                System.out.println(account);
            }

        } else {
            // 태그라인이 없는 경우 db조회 후 없으면 없음



        }

//        Optional<Summoner> summoner = summonerRepository.findSummonerByName(summonerName);
//
//        SummonerData summonerData = null;
//
//        if(summoner.isEmpty()) {
//            // api 호출
//            SummonerDTO summonerDTO = riotClient.getSummoner(summonerName);
//
//        }

        return null;
    }

    public void searchMatchData(String summonerName) throws IOException, InterruptedException {

        Optional<Summoner> summonerEntity = summonerRepository.findSummonerByName(summonerName);

        if(summonerEntity.isEmpty()) {
            // 데이터베이스에 해당 유저가 없으면 api 요청해서 정보를 가져옴
            // 유저 검색 -> matchList 검색 -> matchList 상세 검색 -> db에 데이터넣기
            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create("https://kr.api.riotgames.com/lol/summoner/v4/summoners/by-name/" + summonerName))
                    .headers(riotClient.headers())
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            SummonerDTO summonerDTO = objectMapper.readValue(response.body(), SummonerDTO.class);

            Summoner summoner = summonerRepository.save(summonerDTO.toEntity());

            request = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create("https://kr.api.riotgames.com/lol/league/v4/entries/by-summoner/" + summoner.getId()))
                    .headers(riotClient.headers())
                    .build();

            HttpResponse<String> leagueSummonerResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
            Set<LeagueEntryDTO> leagueEntryDTOS = objectMapper.readValue(leagueSummonerResponse.body(), new TypeReference<Set<LeagueEntryDTO>>() {});

            for (LeagueEntryDTO leagueEntryDTO : leagueEntryDTOS) {

                String leagueId = leagueEntryDTO.getLeagueId();

                Optional<League> leagueEntity = leagueRepository.findById(leagueId);

                if(leagueEntity.isEmpty()) {
                    request = HttpRequest.newBuilder()
                            .GET()
                            .uri(URI.create("https://kr.api.riotgames.com/lol/league/v4/leagues/" + leagueId))
                            .headers(riotClient.headers())
                            .build();

                    HttpResponse<String> leagueResponse = client.send(request, HttpResponse.BodyHandlers.ofString());

                    LeagueListDTO leagueListDTO = objectMapper.readValue(leagueResponse.body(), LeagueListDTO.class);

                    League saveLeague = leagueRepository.save(leagueListDTO.toEntity());

                    LeagueSummoner save = leagueSummonerRepository.save(leagueEntryDTO.toEntity(summoner, saveLeague));

                } else {

                    League league = leagueEntity.get();

                    LeagueSummoner save = leagueSummonerRepository.save(leagueEntryDTO.toEntity(summoner, league));
                }

            }

            request = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create("https://asia.api.riotgames.com/lol/match/v5/matches/by-puuid/"+summoner.getPuuid()+"/ids?start=0&count=20"))
                    .headers(riotClient.headers())
                    .build();

            HttpResponse<String> matchListResponse = client.send(request, HttpResponse.BodyHandlers.ofString());

            List<String> matchList = Arrays.asList(objectMapper.readValue(matchListResponse.body(), String[].class));

            for (String matchId : matchList) {

                request = HttpRequest.newBuilder()
                        .GET()
                        .uri(URI.create("https://asia.api.riotgames.com/lol/match/v5/matches/" + matchId))
                        .headers(riotClient.headers())
                        .build();

                HttpResponse<String> matchResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
                MatchDto matchDTO = objectMapper.readValue(matchResponse.body(), MatchDto.class);

                Match match = matchDTO.toEntity();
                List<TeamDto> teams = matchDTO.getInfo().getTeams();

                // match 가 있으면 등록 x 없으면 등록
                Optional<Match> findMatch = matchRepository.findById(match.getMatchId());

                if(!findMatch.isEmpty()) {
                    continue;
                }

                Match saveMatch = matchRepository.save(match);

                for (TeamDto team : teams) {

                    MatchTeam saveMatchTeam = matchTeamRepository.save(team.toEntity(saveMatch));

                    List<BanDto> bans = team.getBans();

                    for (BanDto ban : bans) {
                        MatchTeamBan saveMatchTeamBan = matchTeamBanRepository.save(new MatchTeamBan(ban.getChampionId(), ban.getPickTurn(), saveMatchTeam));
                    }

                }

                List<ParticipantDto> participants = matchDTO.getInfo().getParticipants();

                for (ParticipantDto participant : participants) {
                    MatchSummoner matchSummoner = participant.toEntity(saveMatch);

                    MatchSummoner save = matchSummonerRepository.save(matchSummoner);
                }
            }

        } else {
            // db 작업 summoner 가 있음

            Summoner summoner = summonerEntity.get();


        }

    }


}
