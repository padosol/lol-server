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

    @QueryProjection
    public MatchTeamDTO(
            String matchId, int teamId, boolean win,
            int championKills, int baronKills,
            int dragonKills, int towerKills,
            int inhibitorKills
    ) {
        this.matchId = matchId;
        this.teamId = teamId;
        this.win = win;
        this.championKills = championKills;
        this.baronKills = baronKills;
        this.dragonKills = dragonKills;
        this.towerKills = towerKills;
        this.inhibitorKills = inhibitorKills;
    }
}
