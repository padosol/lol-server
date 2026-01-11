package com.example.lolserver.domain.summoner.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LeagueSummoner {
    private String puuid;
    private String queue;
    private String leagueId;
    private int wins;
    private int losses;
    private String tier;
    private String rank;
    private int leaguePoints;
    private boolean veteran;
    private boolean inactive;
    private boolean freshBlood;
    private boolean hotStreak;
}
