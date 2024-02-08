package com.example.lolserver.web.dto.data.gameData;

import com.example.lolserver.entity.match.Match;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GameInfoData {

    private String dateVersion;
    private	long gameCreation;
    private	long gameDuration;
    private	long gameEndTimestamp;
    private	long gameId;
    private	String gameMode;
    private	String gameName;
    private	long gameStartTimestamp;
    private	String gameType;
    private	String gameVersion;
    private	int mapId;
    private	String platformId;
    private	int queueId;
    private	String tournamentCode;

    public GameInfoData(Match match) {

        this.dateVersion = match.getDateVersion();
        this.gameCreation = match.getGameCreation();
        this.gameDuration = match.getGameDuration();
        this.gameEndTimestamp = match.getGameEndTimestamp();
        this.gameId = match.getGameId();
        this.gameMode = match.getGameMode();
        this.gameName = match.getGameName();
        this.gameStartTimestamp = match.getGameStartTimestamp();
        this.gameType = match.getGameType();
        this.gameVersion = match.getGameVersion();
        this.mapId = match.getMapId();
        this.platformId = match.getPlatformId();
        this.queueId = match.getQueueId();
        this.tournamentCode = match.getTournamentCode();
    }



}
