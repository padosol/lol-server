package com.example.lolserver.web.service.match;

import com.example.lolserver.entity.match.Match;
import com.example.lolserver.entity.match.MatchSummoner;
import com.example.lolserver.entity.match.MatchTeam;
import com.example.lolserver.entity.match.MatchTeamBan;
import com.example.lolserver.riot.MatchParameters;
import com.example.lolserver.riot.RiotClient;
import com.example.lolserver.riot.dto.match.*;
import com.example.lolserver.web.dto.data.GameData;
import com.example.lolserver.web.dto.data.gameData.GameInfoData;
import com.example.lolserver.web.dto.data.gameData.ParticipantData;
import com.example.lolserver.web.dto.data.gameData.TeamInfoData;
import com.example.lolserver.web.dto.request.MatchRequest;
import com.example.lolserver.web.repository.MatchRepository;
import com.example.lolserver.web.repository.MatchSummonerRepository;
import com.example.lolserver.web.repository.MatchTeamBanRepository;
import com.example.lolserver.web.repository.MatchTeamRepository;
import com.example.lolserver.web.repository.dsl.MatchSummonerRepositoryCustom;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.util.*;

@RequiredArgsConstructor
public abstract class MatchService {

    private final Long START_TIME = 1704844800L;

    protected final RiotClient client;
    protected final MatchRepository matchRepository;
    protected final MatchTeamRepository matchTeamRepository;
    protected final MatchTeamBanRepository matchTeamBanRepository;
    protected final MatchSummonerRepository matchSummonerRepository;
    protected final MatchSummonerRepositoryCustom matchSummonerRepositoryCustom;

    public abstract List<GameData> getMatches(MatchRequest matchRequest) throws IOException, InterruptedException;

    public List<GameData> getMatchesUseRiotApi(MatchRequest matchRequest) throws IOException, InterruptedException {
        List<String> matchList = client.getMatchesByPuuid(matchRequest.getPuuid(), MatchParameters.builder()
                .startTime(START_TIME)
                .queue(matchRequest.getQueueId())
                .build());

        for (String matchId : matchList) {

            MatchDto matchDto = client.getMatchesByMatchId(matchId);

            if(!matchDto.isError()) {
                InfoDto info = matchDto.getInfo();
                List<ParticipantDto> participantDtos = info.getParticipants();
                List<TeamDto> teamDtos = info.getTeams();

                Match match = matchDto.toEntity();
                match.convertEpochToLocalDateTime();

                Optional<Match> findMatchSummoner = matchRepository.findById(matchId);

                if (findMatchSummoner.isEmpty()) {
                    Match saveMatch = matchRepository.save(match);

                    for (ParticipantDto participantDto : participantDtos) {
                        MatchSummoner matchSummoner = participantDto.toEntity(saveMatch);

                        MatchSummoner saveMatchSummoner = matchSummonerRepository.save(matchSummoner);
                    }

                    for (TeamDto teamDto : teamDtos) {
                        MatchTeam matchTeam = teamDto.toEntity(saveMatch);

                        MatchTeam saveMatchTeam = matchTeamRepository.save(matchTeam);

                        List<BanDto> banDtos = teamDto.getBans();

                        for (BanDto banDto : banDtos) {
                            MatchTeamBan matchTeamBan = banDto.toEntity(saveMatchTeam);
                            MatchTeamBan saveMatchTeamBan = matchTeamBanRepository.save(matchTeamBan);
                        }

                    }
                }
            }

        }

        List<MatchSummoner> findMatchSummonerList = matchSummonerRepository.findMatchSummonerByPuuid(matchRequest.getPuuid());

        return createGameData(findMatchSummonerList, matchRequest.getPuuid());
    }

    protected List<GameData> createGameData(List<MatchSummoner> matchSummonerList, String puuid) {

        List<GameData> gameDataList = new ArrayList<>();

        for(MatchSummoner matchSummoner : matchSummonerList) {

            GameData gameData = new GameData();

            List<ParticipantData> participantData = new ArrayList<>();
            List<TeamInfoData> teamInfoDataList = new ArrayList<>();

            // gameInfo
            Match match = matchSummoner.getMatch();
            GameInfoData gameInfoData = new GameInfoData(match);
            gameData.setGameInfoData(gameInfoData);

            // myData
            gameData.setMyData(matchSummoner.toData());

            // participant
            List<MatchSummoner> participants = matchSummonerRepository.findMatchSummonerByMatch(match);

            Map<Integer, Map<String, Integer>> teamKDA = new HashMap<>();

            for(MatchSummoner participant : participants) {
                participantData.add(participant.toData());

                Map<String, Integer> myTeam = teamKDA.get(participant.getTeamId());

            }

            gameData.setParticipantData(participantData);

            // teaminfo
            List<MatchTeam> matchTeamList = matchTeamRepository.findMatchTeamsByMatch(match);

            for(MatchTeam matchTeam : matchTeamList) {
                List<MatchTeamBan> matchTeamBanList = matchTeamBanRepository.findMatchTeamBansByMatchTeam(matchTeam);

                TeamInfoData teamInfoData = new TeamInfoData(matchTeam, matchTeamBanList);
                teamInfoDataList.add(teamInfoData);
            }

            gameData.setTeamInfoData(teamInfoDataList);

            gameDataList.add(gameData);
        }

        return gameDataList;
    }

}
