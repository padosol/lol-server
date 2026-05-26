package com.example.lolserver.match.application.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 게임 정보 읽기 모델. 필드명은 캐시(match:v1) / API JSON 계약이므로 변경 금지.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GameInfoReadModel {

    private String dataVersion;
    private long gameCreation;
    private long gameDuration;
    private long gameEndTimestamp;
    private String gameMode;
    private long gameStartTimestamp;
    private String gameType;
    private String gameVersion;
    private int mapId;
    private String platformId;
    private int queueId;
    private String tournamentCode;
    private String matchId;
    private String averageTier;
    private String averageRank;
}
