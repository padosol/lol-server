package com.example.lolserver.web.dto.data.gameData;


import com.example.lolserver.riot.dto.match.TeamDto;
import com.example.lolserver.web.match.entity.MatchTeam;
import lombok.*;

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

    public TeamInfoData(){};

    public TeamInfoData(TeamDto teamDto) {
        this.teamId = teamDto.getTeamId();
        this.win = teamDto.isWin();
        this.championKills = teamDto.getObjectives().getChampion().getKills();
    }

    public TeamInfoData of(MatchTeam matchTeam) {
        this.teamId = matchTeam.getTeamId();
        this.win = matchTeam.isWin();
        this.championKills = matchTeam.getTeamObject().getChampionKills();

        return this;
    }

}
