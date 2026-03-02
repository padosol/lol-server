package com.example.lolserver.domain.match.domain.gamedata;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TeamInfoData {

    private int teamId;
    private boolean win;

    private int championKills;

    private int baronKills;
    private int dragonKills;
    private int towerKills;
    private int inhibitorKills;
}
