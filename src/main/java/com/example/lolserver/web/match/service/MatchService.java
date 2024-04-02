package com.example.lolserver.web.match.service;

import com.example.lolserver.web.match.entity.Match;
import com.example.lolserver.web.match.entity.MatchSummoner;
import com.example.lolserver.web.match.entity.MatchTeam;
import com.example.lolserver.web.match.entity.MatchTeamBan;
import com.example.lolserver.riot.MatchParameters;
import com.example.lolserver.riot.RiotClient;
import com.example.lolserver.riot.dto.match.*;
import com.example.lolserver.web.dto.data.GameData;
import com.example.lolserver.web.dto.data.gameData.GameInfoData;
import com.example.lolserver.web.dto.data.gameData.ParticipantData;
import com.example.lolserver.web.dto.data.gameData.TeamInfoData;
import com.example.lolserver.web.dto.request.MatchRequest;
import com.example.lolserver.web.match.repository.MatchRepository;
import com.example.lolserver.web.match.repository.MatchSummonerRepository;
import com.example.lolserver.web.match.repository.MatchTeamBanRepository;
import com.example.lolserver.web.match.repository.MatchTeamRepository;
import com.example.lolserver.web.match.repository.dsl.MatchSummonerRepositoryCustom;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;

@RequiredArgsConstructor
public abstract class MatchService {

    // 시즌 시작 일
    private final Long START_TIME = 1704855600L;

    protected final RiotClient client;
    protected final MatchRepository matchRepository;
    protected final MatchTeamRepository matchTeamRepository;
    protected final MatchTeamBanRepository matchTeamBanRepository;
    protected final MatchSummonerRepository matchSummonerRepository;
    protected final MatchSummonerRepositoryCustom matchSummonerRepositoryCustom;

    public abstract List<GameData> getMatches(MatchRequest matchRequest) throws IOException, InterruptedException;

    @Transactional
    public List<GameData> getMatchesUseRiotApi(MatchRequest matchRequest, Pageable pageable) throws IOException, InterruptedException {
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

        Page<MatchSummoner> matchSummoners = matchSummonerRepositoryCustom.findAllByPuuidAndQueueId(matchRequest, pageable);

        return createGameData(matchSummoners.getContent(), matchRequest.getPuuid());
    }

    @Transactional
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
