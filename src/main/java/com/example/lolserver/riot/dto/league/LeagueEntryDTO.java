package com.example.lolserver.riot.dto.league;

import com.example.lolserver.riot.dto.error.ErrorDTO;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class LeagueEntryDTO extends ErrorDTO {

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
