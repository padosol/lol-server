package com.example.lolserver.domain.duo.application.model;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class DuoPostListReadModel {
    private final Long id;
    private final String primaryLane;
    private final String secondaryLane;
    private final boolean hasMicrophone;
    private final String tier;
    private final String rank;
    private final int leaguePoints;
    private final String memo;
    private final String status;
    private final int requestCount;
    private final LocalDateTime expiresAt;
    private final LocalDateTime createdAt;
}
