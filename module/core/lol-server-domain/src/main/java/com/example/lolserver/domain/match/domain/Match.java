package com.example.lolserver.domain.match.domain;

import com.example.lolserver.domain.match.domain.gameData.ParticipantData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Match {
    private String matchId;
    private String dataVersion;
    private List<ParticipantData> participantData; // This will replace matchSummonerEntities

    // info
    private String endOfGameResult;
    private	long gameCreation;
    private	long gameDuration;
    private	long gameEndTimestamp;
    private	long gameStartTimestamp;
    private	long gameId;
    private	String gameMode;
    private	String gameName;
    private	String gameType;

    private	String gameVersion;

    private	int mapId;
    private	int queueId;
    private	String platformId;
    private	String tournamentCode;

    // 시즌
    private int season;

    private Integer averageTier;

    // date time
    private LocalDateTime gameCreateDatetime;
    private LocalDateTime gameEndDatetime;
    private LocalDateTime gameStartDatetime;
}
