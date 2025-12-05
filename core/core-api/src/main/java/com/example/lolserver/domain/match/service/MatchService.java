package com.example.lolserver.domain.match.service;

import com.example.lolserver.domain.match.dto.MSChampionRequest;
import com.example.lolserver.domain.match.dto.MatchRequest;
import com.example.lolserver.storage.db.core.repository.dto.data.GameData;
import com.example.lolserver.storage.db.core.repository.dto.data.TimelineData;
import com.example.lolserver.storage.db.core.repository.match.dto.MSChampionDTO;
import com.example.lolserver.storage.db.core.repository.match.dto.MatchResponse;
import com.example.lolserver.storage.db.core.repository.match.entity.Match;
import com.example.lolserver.storage.db.core.repository.match.entity.timeline.events.ItemEvents;
import com.example.lolserver.storage.db.core.repository.match.entity.timeline.events.SkillEvents;
import com.example.lolserver.storage.db.core.repository.match.match.MatchRepository;
import com.example.lolserver.storage.db.core.repository.match.match.dsl.MatchRepositoryCustom;
import com.example.lolserver.storage.db.core.repository.match.matchsummoner.dsl.MatchSummonerRepositoryCustom;
import com.example.lolserver.storage.db.core.repository.match.timeline.TimelineRepositoryCustom;
import com.example.lolserver.support.error.CoreException;
import com.example.lolserver.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchService {

    private final MatchSummonerRepositoryCustom matchSummonerRepositoryCustom;
    private final MatchRepositoryCustom matchRepositoryCustom;
    private final TimelineRepositoryCustom timelineRepositoryCustom;
    private final MatchRepository matchRepository;

    public MatchResponse getMatches(MatchRequest matchRequest) {

        Pageable pageable = PageRequest.of(matchRequest.getPageNo(), 20, Sort.by(Sort.Direction.DESC, "match"));
        Page<Match> matches = matchRepositoryCustom.getMatches(
                matchRequest.getPuuid(), matchRequest.getQueueId(), pageable);
        List<GameData> gameDataList = matches.getContent().stream().map(match -> match.toGameData(matchRequest.getPuuid())).toList();

        return new MatchResponse(gameDataList, matches.getTotalElements());
    }


    public List<MSChampionDTO> getRankChampions(MSChampionRequest request) {
        return matchSummonerRepositoryCustom.findAllMatchSummonerByPuuidAndSeason(
                request.getPuuid(),
                request.getSeason()
        );
    }


    public GameData getGameData(String matchId) {
        Match match = matchRepository.findById(matchId).orElseThrow(() -> new CoreException(
                ErrorType.NOT_FOUND_MATCH_ID,
                "존재하지 않는 MatchId 입니다. " + matchId
        ));

        return match.toGameData();
    }


    public TimelineData getTimelineData(String matchId) {

        List<ItemEvents> itemEvents = timelineRepositoryCustom.selectAllItemEventsByMatch(matchId);
        List<SkillEvents> skillEvents = timelineRepositoryCustom.selectAllSkillEventsByMatch(matchId);

        return new TimelineData(itemEvents, skillEvents);
    }


    public List<String> findAllMatchIds(MatchRequest matchRequest) {
        Pageable pageable = PageRequest.of(
                matchRequest.getPageNo(),
                20,
                Sort.by(Sort.Direction.DESC, "match")
        );
        Page<String> matchIdsByPuuidWithPage = matchSummonerRepositoryCustom.findAllMatchIdsByPuuidWithPage(
                matchRequest.getPuuid(), matchRequest.getQueueId(), pageable
        );

        return matchIdsByPuuidWithPage.getContent();
    }
}
