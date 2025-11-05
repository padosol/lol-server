package com.example.lolserver.storage.db.core.repository.dto.data.gameData;


import com.example.lolserver.riot.dto.match.TeamDto;
import com.example.lolserver.storage.db.core.repository.match.entity.MatchTeam;
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
