package com.example.lolserver.kafka.messageDto;


import com.example.lolserver.web.league.entity.League;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LeagueMessage {
    private String leagueId;
    private String tier;
    private String name;
    private String queue;

    public LeagueMessage(){};

    public LeagueMessage(League league) {
        this.leagueId = league.getLeagueId();
        this.tier = league.getTier();
        this.name = league.getName();
        this.queue = league.getQueue().name();
    }
}
