package com.example.lolserver.web.match.entity;

import com.example.lolserver.riot.dto.match.MatchDto;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Match {

    @Id
    private String matchId;
    private String dateVersion;

    @OneToMany(mappedBy = "match")
    private List<MatchSummoner> matchSummoners = new ArrayList<>();

//    @OneToMany(mappedBy = "match", fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
//    private List<MatchTeam> matchTeams = new ArrayList<>();

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
        return Match.builder()
                .matchId(matchDto.getMetadata().getMatchId())
                .dateVersion(matchDto.getMetadata().getDataVersion())
                .endOfGameResult(matchDto.getInfo().getEndOfGameResult())
                .gameCreation(matchDto.getInfo().getGameCreation())
                .gameDuration(matchDto.getInfo().getGameDuration())
                .gameEndTimestamp(matchDto.getInfo().getGameEndTimestamp())
                .gameStartTimestamp(matchDto.getInfo().getGameStartTimestamp())
                .gameId(matchDto.getInfo().getGameId())
                .gameMode(matchDto.getInfo().getGameMode())
                .gameName(matchDto.getInfo().getGameName())
                .gameType(matchDto.getInfo().getGameType())
                .gameVersion(matchDto.getInfo().getGameVersion())
                .mapId(matchDto.getInfo().getMapId())
                .queueId(matchDto.getInfo().getQueueId())
                .platformId(matchDto.getInfo().getPlatformId())
                .tournamentCode(matchDto.getInfo().getTournamentCode())
                .season(season)
                .build();

    }

}
