package com.example.lolserver.domain.match.domain.gameData;


import com.example.lolserver.repository.match.entity.MatchTeamEntity;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class TeamInfoData {

    private	int teamId;
    private	boolean win;

    private int championKills;

    private	List<Integer> championId = new ArrayList<>();
    private	List<Integer> pickTurn = new ArrayList<>();

    public TeamInfoData(){}

    public TeamInfoData of(MatchTeamEntity matchTeam) {
        this.teamId = matchTeam.getTeamId();
        this.win = matchTeam.isWin();
        this.championKills = matchTeam.getChampionKills();

        return this;
    }

}
