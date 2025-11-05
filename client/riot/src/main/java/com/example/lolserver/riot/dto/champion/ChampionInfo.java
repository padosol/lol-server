package com.example.lolserver.riot.dto.champion;

import java.util.List;

import com.example.lolserver.riot.dto.error.ErrorDTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChampionInfo extends ErrorDTO {

    private int maxNewPlayerLevel;
    private List<Integer>  freeChampionIdsForNewPlayers;
    private List<Integer>  freeChampionIds;

}
