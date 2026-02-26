package com.example.lolserver.domain.match.application.port.out;

import com.example.lolserver.domain.match.application.model.DailyGameCountReadModel;
import com.example.lolserver.domain.match.application.model.GameReadModel;
import com.example.lolserver.domain.match.domain.MSChampion;
import com.example.lolserver.domain.match.domain.TimelineData;
import com.example.lolserver.support.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MatchPersistencePort {
    Page<GameReadModel> getMatches(String puuid, Integer queueId, Pageable pageable);

    List<MSChampion> getRankChampions(String puuid, Integer season, Integer queueId);

    Optional<GameReadModel> getGameData(String matchId);

    TimelineData getTimelineData(String matchId);

    Page<String> findAllMatchIds(String puuid, Integer queueId, Pageable pageable);

    Page<GameReadModel> getMatchesBatch(String puuid, Integer queueId, Pageable pageable);

    List<DailyGameCountReadModel> getDailyGameCounts(
        String puuid, Integer season, Integer queueId, LocalDateTime startDate);
}
