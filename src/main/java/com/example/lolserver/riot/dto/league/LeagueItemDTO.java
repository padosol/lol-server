package com.example.lolserver.riot.dto.league;

import lombok.Getter;
import lombok.Setter;
import org.example.entity.league.League;
import org.example.entity.league.LeagueSummoner;
import org.example.entity.summoner.Summoner;

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
