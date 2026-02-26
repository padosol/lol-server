package com.example.lolserver.repository.match.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Getter;

@Getter
public class MatchTeamDTO {

    private String matchId;
    private int teamId;
    private boolean win;
    private int championKills;
    private int baronKills;
    private int dragonKills;
    private int towerKills;
    private int inhibitorKills;
    private int champion1Id;
    private int champion2Id;
    private int champion3Id;
    private int champion4Id;
    private int champion5Id;
    private int pick1Turn;
    private int pick2Turn;
    private int pick3Turn;
    private int pick4Turn;
    private int pick5Turn;

    @QueryProjection
    public MatchTeamDTO(
            String matchId, int teamId, boolean win,
            int championKills, int baronKills,
            int dragonKills, int towerKills,
            int inhibitorKills,
            int champion1Id, int champion2Id,
            int champion3Id, int champion4Id,
            int champion5Id,
            int pick1Turn, int pick2Turn,
            int pick3Turn, int pick4Turn,
            int pick5Turn
    ) {
        this.matchId = matchId;
        this.teamId = teamId;
        this.win = win;
        this.championKills = championKills;
        this.baronKills = baronKills;
        this.dragonKills = dragonKills;
        this.towerKills = towerKills;
        this.inhibitorKills = inhibitorKills;
        this.champion1Id = champion1Id;
        this.champion2Id = champion2Id;
        this.champion3Id = champion3Id;
        this.champion4Id = champion4Id;
        this.champion5Id = champion5Id;
        this.pick1Turn = pick1Turn;
        this.pick2Turn = pick2Turn;
        this.pick3Turn = pick3Turn;
        this.pick4Turn = pick4Turn;
        this.pick5Turn = pick5Turn;
    }
}
