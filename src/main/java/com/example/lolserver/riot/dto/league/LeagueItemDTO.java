package com.example.lolserver.riot.dto.league;

import com.example.lolserver.riot.dto.error.ErrorDTO;
import lombok.Getter;

@Getter
public class LeagueItemDTO extends ErrorDTO {

    private	boolean freshBlood;
    private	int wins;
    private	String summonerName;
    private	boolean inactive;
    private	boolean veteran;
    private	boolean hotStreak;
    private	String rank;
    private	int leaguePoints;
    private	int losses;
    private	String summonerId;

}
