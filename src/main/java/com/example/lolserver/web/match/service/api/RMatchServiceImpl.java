package com.example.lolserver.web.match.service.api;

import com.example.lolserver.riot.core.api.RiotAPI;
import com.example.lolserver.riot.dto.match.MatchDto;
import com.example.lolserver.riot.dto.match.ParticipantDto;
import com.example.lolserver.riot.dto.match.TeamDto;
import com.example.lolserver.riot.type.Platform;
import com.example.lolserver.web.dto.data.GameData;
import com.example.lolserver.web.match.dto.MatchRequest;
import com.example.lolserver.web.match.dto.MatchResponse;
import com.example.lolserver.web.match.entity.Match;
import com.example.lolserver.web.match.entity.MatchSummoner;
import com.example.lolserver.web.match.entity.MatchTeam;
import com.example.lolserver.web.match.entity.id.MatchSummonerId;
import com.example.lolserver.web.match.repository.match.MatchRepository;
import com.example.lolserver.web.match.repository.matchsummoner.MatchSummonerRepository;
import com.example.lolserver.web.match.repository.matchteam.MatchTeamRepository;
import com.example.lolserver.web.summoner.entity.Summoner;
import com.example.lolserver.web.summoner.repository.SummonerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RMatchServiceImpl implements RMatchService{

    private final MatchRepository matchRepository;
    private final MatchSummonerRepository matchSummonerRepository;
    private final MatchTeamRepository matchTeamRepository;

    @Override
    @Transactional
    public MatchResponse getMatches(MatchRequest matchRequest) {

        // 최근 20게임
        List<String> matchIds = RiotAPI.matchList(Platform.valueOfName(matchRequest.getPlatform())).byPuuid(matchRequest.getPuuid()).get();

        List<MatchDto> matchDtoList = RiotAPI.match(Platform.valueOfName(matchRequest.getPlatform())).byMatchIds(matchIds);
        
        // match 저장해야함
        // matchSummoner 저장해야함
        // matchTeam 저장해야함

        List<Match> matchList = new ArrayList<>();
        for (MatchDto matchDto : matchDtoList) {
            Match match = matchRepository.save(new Match().of(matchDto, 23));

            List<ParticipantDto> participants = matchDto.getInfo().getParticipants();
            List<TeamDto> teams = matchDto.getInfo().getTeams();

            for (ParticipantDto participant : participants) {
                MatchSummoner matchSummoner = matchSummonerRepository.save(new MatchSummoner().of(match, participant));
                match.addMatchSummoner(matchSummoner);
            }

            for (TeamDto team : teams) {
                MatchTeam matchTeam = matchTeamRepository.save(new MatchTeam().of(match, team));
                match.addMatchTeam(matchTeam);
            }

            matchList.add(match);
        }

        List<GameData> dataList = matchList.stream().map(matchData -> matchData.toGameData(matchRequest.getPuuid())).toList();

        return new MatchResponse(dataList, dataList.size());
    }
}
