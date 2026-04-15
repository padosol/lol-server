package com.example.lolserver.repository.duo.dto;

import com.example.lolserver.domain.duo.domain.vo.MostChampion;
import com.example.lolserver.domain.duo.domain.vo.RecentGameSummary;
import com.querydsl.core.annotations.QueryProjection;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
public class DuoPostListDTO {

    private Long id;
    private String primaryLane;
    private String desiredLane;
    private boolean hasMicrophone;
    private String tier;
    private String rank;
    private int leaguePoints;
    private String memo;
    private String status;
    private long requestCount;
    private List<MostChampion> mostChampions;
    private RecentGameSummary recentGameSummary;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;

    @QueryProjection
    public DuoPostListDTO(Long id, String primaryLane, String desiredLane,
                          boolean hasMicrophone, String tier, String rank,
                          int leaguePoints, String memo, String status,
                          long requestCount, LocalDateTime expiresAt,
                          LocalDateTime createdAt) {
        this.id = id;
        this.primaryLane = primaryLane;
        this.desiredLane = desiredLane;
        this.hasMicrophone = hasMicrophone;
        this.tier = tier;
        this.rank = rank;
        this.leaguePoints = leaguePoints;
        this.memo = memo;
        this.status = status;
        this.requestCount = requestCount;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    public void setMostChampions(List<MostChampion> mostChampions) {
        this.mostChampions = mostChampions;
    }

    public void setRecentGameSummary(RecentGameSummary recentGameSummary) {
        this.recentGameSummary = recentGameSummary;
    }
}
