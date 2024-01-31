package com.example.lolserver.riot.dto.league;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LeagueEntryDTO {

    private String leagueId;
    private String summonerId;
    private String summonerName;
    private String queueType;
    private String tier;
    private String rank;
    private int leaguePoints;
    private int wins;
    private int losses;
    private boolean hotStreak;
    private boolean veteran;
    private boolean freshBlood;
    private boolean inactive;



}
