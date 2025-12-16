package com.example.lolserver.client.summoner.model;

import lombok.Getter;

@Getter
public class LeagueEntryDTO {
    private String leagueId;
    private String summonerId;
    private String puuid;
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
