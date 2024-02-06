package com.example.lolserver.web.service.match;

import com.example.lolserver.entity.match.Match;
import com.example.lolserver.entity.match.MatchSummoner;
import com.example.lolserver.entity.match.MatchTeam;
import com.example.lolserver.entity.match.MatchTeamBan;
import com.example.lolserver.riot.RiotClient;
import com.example.lolserver.riot.dto.match.*;
import com.example.lolserver.web.dto.data.GameData;
import com.example.lolserver.web.dto.data.gameData.GameInfoData;
import com.example.lolserver.web.dto.data.gameData.ParticipantData;
import com.example.lolserver.web.dto.data.gameData.TeamInfoData;
import com.example.lolserver.web.repository.MatchRepository;
import com.example.lolserver.web.repository.MatchSummonerRepository;
import com.example.lolserver.web.repository.MatchTeamBanRepository;
import com.example.lolserver.web.repository.MatchTeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MatchServiceImpl implements MatchService{

    private final RiotClient client;
    private final MatchRepository matchRepository;
    private final MatchTeamRepository matchTeamRepository;
    private final MatchTeamBanRepository matchTeamBanRepository;
    private final MatchSummonerRepository matchSummonerRepository;

    @Override
    public List<GameData> getMatches(String puuid) throws IOException, InterruptedException {

        List<MatchSummoner> matchSummonerList = matchSummonerRepository.findMatchSummonerByPuuid(puuid);

        if (matchSummonerList.size() > 0) {

            List<GameData> gameData = createGameData(matchSummonerList, puuid);

            return gameData;
        }

        List<String> matchList = client.getMatchesByPuuid(puuid);

        for (String matchId : matchList) {

            MatchDto matchDto = client.getMatchesByMatchId(matchId);
            InfoDto info = matchDto.getInfo();
            List<ParticipantDto> participantDtos = info.getParticipants();
            List<TeamDto> teamDtos = info.getTeams();

            Match match = matchDto.toEntity();

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

        List<MatchSummoner> findMatchSummonerList = matchSummonerRepository.findMatchSummonerByPuuid(puuid);

        List<GameData> gameData = createGameData(findMatchSummonerList, puuid);

        return gameData;
    }

    private List<GameData> createGameData(List<MatchSummoner> matchSummonerList, String puuid) {

        List<GameData> gameDataList = new ArrayList<>();

        for(MatchSummoner matchSummoner : matchSummonerList) {

            GameData gameData = new GameData();

            GameInfoData gameInfoData = new GameInfoData();
            TeamInfoData teamInfoData = new TeamInfoData();
            List<ParticipantData> participantData = new ArrayList<>();

            // gameInfo
            Match match = matchSummoner.getMatch();

            // myData
            gameData.setMyData(matchSummoner.toData());

            // participant
            List<MatchSummoner> participants = matchSummonerRepository.findMatchSummonerByMatch(match);

            for(MatchSummoner participant : participants) {
                participantData.add(participant.toData());
            }

            gameData.setParticipantData(participantData);

            // teaminfo
            List<MatchTeam> matchTeamList = matchTeamRepository.findMatchTeamsByMatch(match);

            for(MatchTeam matchTeam : matchTeamList) {
                Optional<MatchTeamBan> matchTeamBan = matchTeamBanRepository.findMatchTeamBanByMatchTeam(matchTeam);

            }

            gameDataList.add(gameData);
        }

        return gameDataList;
    }

}
