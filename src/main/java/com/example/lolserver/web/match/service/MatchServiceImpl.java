package com.example.lolserver.web.match.service;

import com.example.lolserver.web.dto.data.GameData;
import com.example.lolserver.web.dto.data.TimelineData;
import com.example.lolserver.web.match.dto.MSChampionRequest;
import com.example.lolserver.web.match.dto.MSChampionResponse;
import com.example.lolserver.web.match.dto.MatchRequest;
import com.example.lolserver.web.match.dto.MatchResponse;
import com.example.lolserver.web.match.entity.Match;
import com.example.lolserver.web.match.entity.MatchSummoner;
import com.example.lolserver.web.match.entity.timeline.TimeLineEvent;
import com.example.lolserver.web.match.entity.timeline.events.ItemEvents;
import com.example.lolserver.web.match.entity.timeline.events.SkillEvents;
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
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchServiceImpl implements MatchService {

    private final MatchSummonerRepositoryCustom matchSummonerRepositoryCustom;
    private final MatchRepositoryCustom matchRepositoryCustom;
    private final TimelineRepositoryCustom timelineRepositoryCustom;

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
    public TimelineData getTimelineData(String matchId) {

        List<ItemEvents> itemEvents = timelineRepositoryCustom.selectAllItemEventsByMatch(matchId);
        List<SkillEvents> skillEvents = timelineRepositoryCustom.selectAllSkillEventsByMatch(matchId);

        return new TimelineData(itemEvents, skillEvents);
    }
}
