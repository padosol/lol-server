package com.example.lolserver.web.dto.data.gameData;


import com.example.lolserver.entity.match.MatchTeam;
import com.example.lolserver.entity.match.MatchTeamBan;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class TeamInfoData {

    private	int teamId;
    private	boolean win;

    private	List<Integer> championId = new ArrayList<>();
    private	List<Integer> pickTurn = new ArrayList<>();

    public TeamInfoData(MatchTeam matchTeam, List<MatchTeamBan> matchTeamBanList) {
        this.teamId = matchTeam.getTeamId();
        this.win = matchTeam.isWin();

        for(MatchTeamBan matchTeamBan : matchTeamBanList) {
            this.championId.add(matchTeamBan.getChampionId());
            this.pickTurn.add(matchTeamBan.getPickTurn());
        }

    }


}
