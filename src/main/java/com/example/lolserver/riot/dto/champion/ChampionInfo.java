package com.example.lolserver.riot.dto.champion;

import com.example.lolserver.riot.dto.error.ErrorDTO;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ChampionInfo extends ErrorDTO {

    private int maxNewPlayerLevel;
    private List<Integer>  freeChampionIdsForNewPlayers;
    private List<Integer>  freeChampionIds;

}
