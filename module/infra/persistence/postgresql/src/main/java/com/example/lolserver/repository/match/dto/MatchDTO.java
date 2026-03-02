package com.example.lolserver.repository.match.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Getter;

@Getter
public class MatchDTO {

    private String matchId;
    private String dataVersion;
    private long gameCreation;
    private long gameDuration;
    private long gameEndTimestamp;
    private long gameStartTimestamp;
    private String gameMode;
    private String gameType;
    private String gameVersion;
    private int mapId;
    private int queueId;
    private String platformId;
    private String tournamentCode;
    private Integer averageTier;

    @QueryProjection
    public MatchDTO(
            String matchId, String dataVersion,
            long gameCreation, long gameDuration,
            long gameEndTimestamp, long gameStartTimestamp,
            String gameMode, String gameType,
            String gameVersion, int mapId,
            int queueId, String platformId,
            String tournamentCode, Integer averageTier
    ) {
        this.matchId = matchId;
        this.dataVersion = dataVersion;
        this.gameCreation = gameCreation;
        this.gameDuration = gameDuration;
        this.gameEndTimestamp = gameEndTimestamp;
        this.gameStartTimestamp = gameStartTimestamp;
        this.gameMode = gameMode;
        this.gameType = gameType;
        this.gameVersion = gameVersion;
        this.mapId = mapId;
        this.queueId = queueId;
        this.platformId = platformId;
        this.tournamentCode = tournamentCode;
        this.averageTier = averageTier;
    }
}
