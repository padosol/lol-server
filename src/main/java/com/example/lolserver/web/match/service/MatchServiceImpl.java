package com.example.lolserver.web.match.service;

import com.example.lolserver.riot.type.Platform;
import com.example.lolserver.web.dto.data.GameData;
import com.example.lolserver.web.dto.data.TimelineData;
import com.example.lolserver.web.exception.WebException;
import com.example.lolserver.web.match.dto.MSChampionRequest;
import com.example.lolserver.web.match.dto.MSChampionResponse;
import com.example.lolserver.web.match.dto.MatchRequest;
import com.example.lolserver.web.match.dto.MatchResponse;
import com.example.lolserver.web.match.entity.Match;
import com.example.lolserver.web.match.entity.MatchSummoner;
import com.example.lolserver.web.match.entity.timeline.TimeLineEvent;
import com.example.lolserver.web.match.entity.timeline.events.ItemEvents;
import com.example.lolserver.web.match.entity.timeline.events.SkillEvents;
import com.example.lolserver.web.match.repository.match.MatchRepository;
import com.example.lolserver.web.match.repository.match.dsl.MatchRepositoryCustom;
import com.example.lolserver.web.match.repository.matchsummoner.dsl.MatchSummonerRepositoryCustom;
import com.example.lolserver.web.match.repository.timeline.TimelineRepositoryCustom;
import com.example.lolserver.web.match.service.api.RMatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchServiceImpl implements MatchService {


    private final MatchSummonerRepositoryCustom matchSummonerRepositoryCustom;
    private final MatchRepositoryCustom matchRepositoryCustom;
    private final TimelineRepositoryCustom timelineRepositoryCustom;
    private final MatchRepository matchRepository;

    @Override
    public MatchResponse getMatches(MatchRequest matchRequest) {

        Pageable pageable = PageRequest.of(matchRequest.getPageNo(), 20, Sort.by(Sort.Direction.DESC, "match"));
        Page<Match> matches = matchRepositoryCustom.getMatches(matchRequest, pageable);
        List<GameData> gameDataList = matches.getContent().stream().map(match -> match.toGameData(matchRequest.getPuuid())).toList();

        return new MatchResponse(gameDataList, matches.getTotalElements());
    }

    @Override
    public List<MSChampionResponse> getRankChampions(MSChampionRequest request) {

        return matchSummonerRepositoryCustom.findAllChampionKDAByPuuidAndSeasonAndQueueType(
                request.getPuuid(),
                request.getSeason(),
                request.getQueueId(),
                7L
        );
    }

    @Override
    public GameData getGameData(String matchId) {
        Match match = matchRepository.findById(matchId).orElseThrow(() -> new WebException(
                HttpStatus.BAD_REQUEST,
                "존재하지 않는 MatchId 입니다. " + matchId
        ));

        return match.toGameData();
    }

    @Override
    public TimelineData getTimelineData(String matchId) {

        List<ItemEvents> itemEvents = timelineRepositoryCustom.selectAllItemEventsByMatch(matchId);
        List<SkillEvents> skillEvents = timelineRepositoryCustom.selectAllSkillEventsByMatch(matchId);

        return new TimelineData(itemEvents, skillEvents);
    }

    @Override
    public List<String> findAllMatchIds(MatchRequest matchRequest) {
        Pageable pageable = PageRequest.of(
                matchRequest.getPageNo(),
                20,
                Sort.by(Sort.Direction.DESC, "match")
        );
        Page<String> matchIdsByPuuidWithPage = matchSummonerRepositoryCustom.findAllMatchIdsByPuuidWithPage(
                matchRequest, pageable
        );

        return matchIdsByPuuidWithPage.getContent();
    }
}
