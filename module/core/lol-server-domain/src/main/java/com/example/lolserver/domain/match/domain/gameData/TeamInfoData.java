package com.example.lolserver.domain.match.domain.gameData;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TeamInfoData {

    private	int teamId;
    private	boolean win;

    private int championKills;

    private	List<Integer> championId = new ArrayList<>();
    private	List<Integer> pickTurn = new ArrayList<>();
}
