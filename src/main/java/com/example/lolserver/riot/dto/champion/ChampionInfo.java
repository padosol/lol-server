package com.example.lolserver.riot.dto.champion;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ChampionInfo {

    private int maxNewPlayerLevel;
    private List<Integer>  freeChampionIdsForNewPlayers;
    private List<Integer>  freeChampionIds;

}
