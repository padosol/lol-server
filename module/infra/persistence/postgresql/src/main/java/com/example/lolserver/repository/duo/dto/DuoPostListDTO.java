package com.example.lolserver.repository.duo.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class DuoPostListDTO {

    private Long id;
    private String primaryLane;
    private String secondaryLane;
    private boolean hasMicrophone;
    private String tier;
    private String rank;
    private int leaguePoints;
    private String memo;
    private String status;
    private long requestCount;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;

    @QueryProjection
    public DuoPostListDTO(Long id, String primaryLane, String secondaryLane,
                          boolean hasMicrophone, String tier, String rank,
                          int leaguePoints, String memo, String status,
                          long requestCount, LocalDateTime expiresAt,
                          LocalDateTime createdAt) {
        this.id = id;
        this.primaryLane = primaryLane;
        this.secondaryLane = secondaryLane;
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
}
