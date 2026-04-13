package com.example.lolserver.domain.match.application.port.out;

import com.example.lolserver.domain.match.application.model.DailyGameCountReadModel;
import com.example.lolserver.domain.match.application.model.GameReadModel;
import com.example.lolserver.domain.match.domain.MSChampion;
import com.example.lolserver.domain.match.domain.TimelineData;
import com.example.lolserver.support.PaginationRequest;
import com.example.lolserver.support.SliceResult;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MatchPersistencePort {
    SliceResult<GameReadModel> getMatches(String puuid, Integer queueId, PaginationRequest paginationRequest);

    List<MSChampion> getRankChampions(String puuid, Integer season, Integer queueId);

    Optional<GameReadModel> getGameData(String matchId);

    TimelineData getTimelineData(String matchId);

    SliceResult<String> findAllMatchIds(String puuid, Integer queueId, PaginationRequest paginationRequest);

    SliceResult<GameReadModel> getMatchesBatch(String puuid, Integer season, Integer queueId, PaginationRequest paginationRequest);

    List<DailyGameCountReadModel> getDailyGameCounts(
        String puuid, Integer season, Integer queueId, LocalDateTime startDate);
}
