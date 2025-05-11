package com.example.lolserver.web.match.repository.matchsummoner.dsl;

import com.example.lolserver.web.match.dto.MSChampionResponse;
import com.example.lolserver.web.match.dto.MatchRequest;
import com.example.lolserver.web.match.dto.LinePosition;
import com.example.lolserver.web.match.entity.MatchSummoner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface MatchSummonerRepositoryCustom {

    Page<MatchSummoner> findAllByPuuidAndQueueId(MatchRequest matchRequest, Pageable pageable);

    List<String> findAllByMatchIdNotExist(List<String> matchIds);

    List<MSChampionResponse> findAllChampionKDAByPuuidAndSeasonAndQueueType(String puuid, Integer season, Integer queueType, Long limit);

    List<LinePosition> findAllPositionByPuuidAndLimit(String puuid, Long limit);

    Page<String> findAllMatchIdsByPuuidWithPage(MatchRequest matchRequest, Pageable pageable);
}
