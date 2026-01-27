package com.example.lolserver.repository.match.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "match")
public class MatchEntity {

    @Id
    @Column(name = "match_id")
    private String matchId;

    @Column(name = "data_version")
    private String dataVersion;

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

    // date time
    private LocalDateTime gameCreateDatetime;
    private LocalDateTime gameEndDatetime;
    private LocalDateTime gameStartDatetime;

    // timeline
//    @BatchSize(size = 200)
//    @OneToMany(mappedBy = "match")
//    private List<TimeLineEvent> timeLineEvents;

//    public void addMatchSummoner(MatchSummoner matchSummoner) {
//        if(this.matchSummoners == null) {
//            this.matchSummoners = new ArrayList<>();
//        }
//
//        this.matchSummoners.add(matchSummoner);
//    }
//
//    public void addMatchTeam(MatchTeam matchTeam) {
//        if(this.matchTeams == null) {
//            this.matchTeams = new ArrayList<>();
//        }
//
//        this.matchTeams.add(matchTeam);
//    }
//
//    public Match of(MatchDto matchDto, int season) {
//
//        return Match.builder()
//                .matchId(matchDto.getMetadata().getMatchId())
//                .dateVersion(matchDto.getMetadata().getDataVersion())
//                .endOfGameResult(matchDto.getInfo().getEndOfGameResult())
//                .gameCreation(matchDto.getInfo().getGameCreation())
//                .gameDuration(matchDto.getInfo().getGameDuration())
//                .gameEndTimestamp(matchDto.getInfo().getGameEndTimestamp())
//                .gameStartTimestamp(matchDto.getInfo().getGameStartTimestamp())
//                .gameId(matchDto.getInfo().getGameId())
//                .gameMode(matchDto.getInfo().getGameMode())
//                .gameName(matchDto.getInfo().getGameName())
//                .gameType(matchDto.getInfo().getGameType())
//                .gameVersion(matchDto.getInfo().getGameVersion())
//                .mapId(matchDto.getInfo().getMapId())
//                .queueId(matchDto.getInfo().getQueueId())
//                .platformId(matchDto.getInfo().getPlatformId())
//                .tournamentCode(matchDto.getInfo().getTournamentCode())
//                .season(season)
//                .gameCreateDatetime(LocalDateTime.ofInstant(Instant.ofEpochMilli(matchDto.getInfo().getGameCreation()), ZoneId.systemDefault()))
//                .gameEndDatetime(LocalDateTime.ofInstant(Instant.ofEpochMilli(matchDto.getInfo().getGameEndTimestamp()), ZoneId.systemDefault()))
//                .gameStartDatetime(LocalDateTime.ofInstant(Instant.ofEpochMilli(matchDto.getInfo().getGameStartTimestamp()), ZoneId.systemDefault()))
//                .build();
//    }
//
//
//    public boolean isGameResultOk() {
//        return this.endOfGameResult.equals("GameComplete");
//    }
//
//    public boolean isAbortUnexpected() {
//        return this.endOfGameResult.equals("Abort_TooFewPlayers");
//    }
//
//
//    public boolean isGameId() {
//        return this.gameId != 0;
//    }

}
