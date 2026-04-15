package com.example.lolserver.domain.duo.application.model;

import com.example.lolserver.domain.duo.domain.vo.MostChampion;
import com.example.lolserver.domain.duo.domain.vo.RecentGameSummary;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class DuoPostListReadModel {
    private final Long id;
    private final String primaryLane;
    private final String desiredLane;
    private final boolean hasMicrophone;
    private final String tier;
    private final String rank;
    private final int leaguePoints;
    private final String memo;
    private final String status;
    private final int requestCount;
    private final List<MostChampion> mostChampions;
    private final RecentGameSummary recentGameSummary;
    private final LocalDateTime expiresAt;
    private final LocalDateTime createdAt;
}
