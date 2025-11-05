package com.example.lolserver.storage.db.core.repository.match.matchsummoner.dsl;

import com.example.lolserver.storage.db.core.repository.match.dto.LinePosition;
import com.example.lolserver.storage.db.core.repository.match.dto.MSChampionResponse;
import com.example.lolserver.storage.db.core.repository.match.entity.MatchSummoner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface MatchSummonerRepositoryCustom {

    Page<MatchSummoner> findAllByPuuidAndQueueId(String puuid, Integer queueId, Pageable pageable);

    List<String> findAllByMatchIdNotExist(List<String> matchIds);

    List<MSChampionResponse> findAllChampionKDAByPuuidAndSeasonAndQueueType(String puuid, Integer season, Integer queueType, Long limit);

    List<LinePosition> findAllPositionByPuuidAndLimit(String puuid, Long limit);

    Page<String> findAllMatchIdsByPuuidWithPage(String puuid, Integer queueId, Pageable pageable);
}
