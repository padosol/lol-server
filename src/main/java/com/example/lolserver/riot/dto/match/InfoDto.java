package com.example.lolserver.riot.dto.match;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InfoDto {

    private String endOfGameResult;
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
    private List<ParticipantDto> participants;
    private	String platformId;
    private	int queueId;
    private	List<TeamDto> teams;
    private	String tournamentCode;


}
