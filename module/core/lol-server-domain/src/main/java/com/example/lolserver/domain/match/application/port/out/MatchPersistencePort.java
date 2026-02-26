package com.example.lolserver.domain.match.application.port.out;

import com.example.lolserver.domain.match.application.dto.DailyGameCountResponse;
import com.example.lolserver.domain.match.application.dto.GameResponse;
import com.example.lolserver.domain.match.domain.MSChampion;
import com.example.lolserver.domain.match.domain.TimelineData;
import com.example.lolserver.support.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MatchPersistencePort {
    Page<GameResponse> getMatches(String puuid, Integer queueId, Pageable pageable);

    List<MSChampion> getRankChampions(String puuid, Integer season, Integer queueId);

    Optional<GameResponse> getGameData(String matchId);

    TimelineData getTimelineData(String matchId);

    Page<String> findAllMatchIds(String puuid, Integer queueId, Pageable pageable);

    Page<GameResponse> getMatchesBatch(String puuid, Integer queueId, Pageable pageable);

    List<DailyGameCountResponse> getDailyGameCounts(
        String puuid, Integer season, Integer queueId, LocalDateTime startDate);
}
