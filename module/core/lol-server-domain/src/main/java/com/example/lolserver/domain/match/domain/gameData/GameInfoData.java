package com.example.lolserver.domain.match.domain.gameData;

import com.example.lolserver.repository.match.entity.MatchEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GameInfoData {

    private String dataVersion;
    private	long gameCreation;
    private	long gameDuration;
    private	long gameEndTimestamp;
    private	String gameMode;
    private	long gameStartTimestamp;
    private	String gameType;
    private	String gameVersion;
    private	int mapId;
    private	String platformId;
    private	int queueId;
    private	String tournamentCode;
    private String matchId;

    public GameInfoData(){}
    public GameInfoData(MatchEntity match) {

        this.dataVersion = match.getDataVersion();
        this.gameCreation = match.getGameCreation();
        this.gameDuration = match.getGameDuration();
        this.gameEndTimestamp = match.getGameEndTimestamp();
        this.gameMode = match.getGameMode();
        this.gameStartTimestamp = match.getGameStartTimestamp();
        this.gameType = match.getGameType();
        this.gameVersion = match.getGameVersion();
        this.mapId = match.getMapId();
        this.platformId = match.getPlatformId();
        this.queueId = match.getQueueId();
        this.tournamentCode = match.getTournamentCode();
        this.matchId = match.getMatchId();
    }
}
