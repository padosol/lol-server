package com.example.lolserver.match.application.port.out;

import com.example.lolserver.match.application.model.DailyGameCountReadModel;
import com.example.lolserver.match.application.model.GameReadModel;
import com.example.lolserver.match.domain.MSChampionByQueue;
import com.example.lolserver.match.domain.TimelineData;
import com.example.lolserver.common.support.PaginationRequest;
import com.example.lolserver.common.support.SliceResult;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MatchPersistencePort {
    SliceResult<GameReadModel> getMatches(String puuid, Integer queueId, PaginationRequest paginationRequest);

    MSChampionByQueue getRankChampions(String puuid, Integer season);

    Optional<GameReadModel> getGameData(String matchId);

    TimelineData getTimelineData(String matchId);

    SliceResult<String> findAllMatchIds(String puuid, Integer queueId, PaginationRequest paginationRequest);

    SliceResult<GameReadModel> getMatchesBatch(
            String puuid, Integer season, Integer queueId,
            PaginationRequest paginationRequest);

    /**
     * puuid 의 최근 matchId 를 gameEndTimestamp DESC 순으로 limit 개 반환한다 (2-tier 캐시의 read-through 용).
     */
    List<String> findRecentMatchIds(String puuid, int limit);

    /**
     * 주어진 matchId 목록에 대한 GameReadModel 을 일괄 조회한다 (2-tier 캐시의 read-through 용).
     */
    List<GameReadModel> findMatchesByIds(Collection<String> matchIds);

    List<DailyGameCountReadModel> getDailyGameCounts(
        String puuid, Integer season, Integer queueId, LocalDateTime startDate);
}
