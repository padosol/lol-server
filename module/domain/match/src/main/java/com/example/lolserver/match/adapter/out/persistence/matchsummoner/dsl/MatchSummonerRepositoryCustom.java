package com.example.lolserver.match.adapter.out.persistence.matchsummoner.dsl;

import com.example.lolserver.match.adapter.out.persistence.dto.DailyGameCountDTO;
import com.example.lolserver.match.adapter.out.persistence.dto.LinePositionDTO;
import com.example.lolserver.match.adapter.out.persistence.dto.MSChampionDTO;
import com.example.lolserver.match.adapter.out.persistence.entity.MatchSummonerEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.time.LocalDateTime;
import java.util.List;

public interface MatchSummonerRepositoryCustom {

    Page<MatchSummonerEntity> findAllByPuuidAndQueueId(String puuid, Integer queueId, Pageable pageable);

    List<String> findAllByMatchIdNotExist(List<String> matchIds);

    List<MSChampionDTO> findAllChampionKDAByPuuidAndSeasonAndQueueType(String puuid, Integer season);

    List<MSChampionDTO> findAllRankedMatchSummonerByPuuidAndSeason(String puuid, Integer season);

    List<LinePositionDTO> findAllPositionByPuuidAndLimit(String puuid, Long limit);

    Slice<String> findAllMatchIdsByPuuidWithPage(String puuid, Integer queueId, Pageable pageable);

    List<DailyGameCountDTO> findDailyGameCounts(
        String puuid, Integer season, Integer queueId, LocalDateTime startDate);
}
