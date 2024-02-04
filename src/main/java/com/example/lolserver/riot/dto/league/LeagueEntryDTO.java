package com.example.lolserver.riot.dto.league;

import com.example.lolserver.entity.league.League;
import com.example.lolserver.entity.league.LeagueSummoner;
import com.example.lolserver.entity.league.QueueType;
import com.example.lolserver.entity.summoner.Summoner;
import com.example.lolserver.riot.dto.error.ErrorDTO;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LeagueEntryDTO extends ErrorDTO {

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

    public LeagueSummoner toEntity(Summoner summoner, League league) {

         return LeagueSummoner.builder()
                 .leaguePoints(leaguePoints)
                 .rank(rank)
                 .wins(wins)
                 .losses(losses)
                 .veteran(veteran)
                 .inactive(inactive)
                 .freshBlood(freshBlood)
                 .hotStreak(hotStreak)
                 .summoner(summoner)
                 .league(league)
                 .build();
    }

}
