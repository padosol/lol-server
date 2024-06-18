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
import com.example.lolserver.web.match.repository.match.dsl.MatchRepositoryCustom;
import com.example.lolserver.web.match.repository.matchsummoner.MatchSummonerRepository;
import com.example.lolserver.web.match.repository.matchteam.MatchTeamRepository;
import com.example.lolserver.web.summoner.entity.Summoner;
import com.example.lolserver.web.summoner.repository.SummonerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class RMatchServiceImpl implements RMatchService{

    private final MatchRepository matchRepository;
    private final MatchSummonerRepository matchSummonerRepository;
    private final MatchTeamRepository matchTeamRepository;
    private final MatchRepositoryCustom matchRepositoryCustom;

    @Override
    @Transactional
    public MatchResponse getMatches(MatchRequest matchRequest) {

        // 최근 20게임
        List<String> matchIds = RiotAPI.matchList(Platform.valueOfName(matchRequest.getPlatform()))
                .byPuuid(matchRequest.getPuuid())
                .query(matchQueryBuilder -> matchQueryBuilder.queue(matchRequest.getQueueId()).build())
                .get();

        List<MatchDto> matchDtoList = RiotAPI.match(Platform.valueOfName(matchRequest.getPlatform())).byMatchIds(matchIds);
        
        // match 저장해야함
        // matchSummoner 저장해야함
        // matchTeam 저장해야함
        List<Match> matchList = insertMatches(matchDtoList);

        List<GameData> dataList = matchList.stream().map(matchData -> matchData.toGameData(matchRequest.getPuuid())).toList();

        return new MatchResponse(dataList, (long) dataList.size());
    }

    @Override
    @Transactional
    public List<Match> insertMatches(List<MatchDto> matchDtoList) {
        return bulkInsertMatches(matchDtoList);
    }

    public List<Match> bulkInsertMatches(List<MatchDto> matchDtoList) {

        List<Match> matchList = new ArrayList<>();
        List<MatchSummoner> matchSummonerList = new ArrayList<>();
        List<MatchTeam> matchTeamList = new ArrayList<>();

        for (MatchDto matchDto : matchDtoList) {

            if(matchDto.isError()) {
                continue;
            }

            Match match = new Match().of(matchDto, 23);

            List<ParticipantDto> participants = matchDto.getInfo().getParticipants();
            List<TeamDto> teams = matchDto.getInfo().getTeams();

            for (ParticipantDto participant : participants) {
                MatchSummoner matchSummoner = new MatchSummoner().of(match, participant);
                match.addMatchSummoner(matchSummoner);

                matchSummonerList.add(matchSummoner);
            }

            for (TeamDto team : teams) {
                MatchTeam matchTeam = new MatchTeam().of(match, team);
                match.addMatchTeam(matchTeam);

                matchTeamList.add(matchTeam);
            }

            matchList.add(match);
        }

        matchRepositoryCustom.matchBulkInsert(matchList);

        return matchList;
    }
}
