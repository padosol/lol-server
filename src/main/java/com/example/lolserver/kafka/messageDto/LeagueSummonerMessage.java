package com.example.lolserver.kafka.messageDto;

import java.time.LocalDateTime;

import com.example.lolserver.web.league.entity.LeagueSummoner;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LeagueSummonerMessage {

    private String summonerId;
    private String leagueId;
    private LocalDateTime createAt;

    private int leaguePoints;
    private String rank;
    private int wins;
    private int losses;
    private boolean veteran;
    private boolean inactive;
    private boolean freshBlood;
    private boolean hotStreak;

    public LeagueSummonerMessage(){};

    public LeagueSummonerMessage(LeagueSummoner leagueSummoner) {
        this.summonerId = leagueSummoner.getId().getSummonerId();
        this.leagueId = leagueSummoner.getId().getLeagueId();
        this.createAt = leagueSummoner.getId().getCreateAt();

        this.leaguePoints = leagueSummoner.getLeaguePoints();
        this.rank = leagueSummoner.getRank();
        this.wins = leagueSummoner.getWins();
        this.losses = leagueSummoner.getLosses();
        this.veteran = leagueSummoner.isVeteran();
        this.inactive = leagueSummoner.isInactive();
        this.freshBlood = leagueSummoner.isFreshBlood();
        this.hotStreak = leagueSummoner.isHotStreak();
    }

}
