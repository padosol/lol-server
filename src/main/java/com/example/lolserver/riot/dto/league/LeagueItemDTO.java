package com.example.lolserver.riot.dto.league;

import com.example.lolserver.entity.league.League;
import com.example.lolserver.entity.league.LeagueSummoner;
import com.example.lolserver.web.summoner.entity.Summoner;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LeagueItemDTO {

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

    public LeagueSummoner toEntity(League league, Summoner summoner) {

        return LeagueSummoner.builder()
                .leaguePoints(leaguePoints)
                .rank(rank)
                .wins(wins)
                .losses(losses)
                .veteran(veteran)
                .inactive(inactive)
                .freshBlood(freshBlood)
                .hotStreak(hotStreak)
                .league(league)
                .summoner(summoner)
                .build();
    }

}
