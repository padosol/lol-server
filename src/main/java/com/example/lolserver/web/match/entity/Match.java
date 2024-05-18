package com.example.lolserver.web.match.entity;

import com.example.lolserver.riot.dto.match.MatchDto;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Match {

    // metaData
    @Id
    private String matchId;
    private String dateVersion;

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

    public Match of(MatchDto matchDto, int season) {

        return new Match(
            matchDto.getMetadata().getMatchId(),
            matchDto.getMetadata().getDataVersion(),
            matchDto.getInfo().getEndOfGameResult(),
            matchDto.getInfo().getGameCreation(),
            matchDto.getInfo().getGameDuration(),
            matchDto.getInfo().getGameEndTimestamp(),
            matchDto.getInfo().getGameStartTimestamp(),
            matchDto.getInfo().getGameId(),
            matchDto.getInfo().getGameMode(),
            matchDto.getInfo().getGameName(),
            matchDto.getInfo().getGameType(),
            matchDto.getInfo().getGameVersion(),
            matchDto.getInfo().getMapId(),
            matchDto.getInfo().getQueueId(),
            matchDto.getInfo().getPlatformId(),
            matchDto.getInfo().getTournamentCode(),
            season
        );
    }

}
