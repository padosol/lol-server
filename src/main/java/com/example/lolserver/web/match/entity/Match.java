package com.example.lolserver.web.match.entity;

import com.example.lolserver.riot.dto.match.MatchDto;
import com.example.lolserver.web.dto.data.GameData;
import com.example.lolserver.web.dto.data.gameData.GameInfoData;
import com.example.lolserver.web.dto.data.gameData.ParticipantData;
import com.example.lolserver.web.dto.data.gameData.TeamInfoData;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.type.MapType;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Match {

    @Id
    @Column(name = "match_id")
    private String matchId;

    private String dateVersion;

    @BatchSize(size = 100)
    @OneToMany(mappedBy = "match", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MatchSummoner> matchSummoners;

    @BatchSize(size = 20)
    @OneToMany(mappedBy = "match", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MatchTeam> matchTeams;

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

    public void addMatchSummoner(MatchSummoner matchSummoner) {
        if(this.matchSummoners == null) {
            this.matchSummoners = new ArrayList<>();
        }

        this.matchSummoners.add(matchSummoner);
    }

    public void addMatchTeam(MatchTeam matchTeam) {
        if(this.matchTeams == null) {
            this.matchTeams = new ArrayList<>();
        }

        this.matchTeams.add(matchTeam);
    }

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
                .gameCreateDatetime(LocalDateTime.ofInstant(Instant.ofEpochMilli(this.gameCreation), ZoneId.systemDefault()))
                .gameEndDatetime(LocalDateTime.ofInstant(Instant.ofEpochMilli(this.gameEndTimestamp), ZoneId.systemDefault()))
                .gameStartDatetime(LocalDateTime.ofInstant(Instant.ofEpochMilli(this.gameStartTimestamp), ZoneId.systemDefault()))
                .build();
    }

    public GameData toGameData(String puuid) {

        GameData gameData = new GameData();

        // 게임 정보
        GameInfoData gameInfoData = new GameInfoData(this);
        gameData.setGameInfoData(gameInfoData);

        // 유저 정보
        List<ParticipantData> participantData = new ArrayList<>();
        for (MatchSummoner matchSummoner : this.matchSummoners) {
            ParticipantData data = new ParticipantData().of(matchSummoner);
            participantData.add(data);

            if(data.getPuuid().equals(puuid)) {
                gameData.setMyData(data);
            }
        }
        gameData.setParticipantData(participantData);

        // 팀정보
        Map<Integer, TeamInfoData> teamInfoDataMap = new HashMap<>();
        for (MatchTeam matchTeam : this.matchTeams) {
            teamInfoDataMap.put(matchTeam.getTeamId(), new TeamInfoData().of(matchTeam));
        }
        gameData.setTeamInfoData(teamInfoDataMap);

        return gameData;
    }

    public boolean isGameResultOk() {
        return this.endOfGameResult.equals("GameComplete");
    }

    public boolean isAbortUnexpected() {
        return this.endOfGameResult.equals("Abort_Unexpected");
    }

}
