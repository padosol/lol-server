package com.example.lolserver.domain.champion.domain;

import com.example.lolserver.client.summoner.model.ChampionInfo;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
public class ChampionRotate {

    private int maxNewPlayerLevel;
    private List<Integer> freeChampionIdsForNewPlayers;
    private List<Integer> freeChampionIds;

    public static ChampionRotate of(ChampionInfo championInfo) {
        return new ChampionRotate(
                championInfo.maxNewPlayerLevel(),
                championInfo.freeChampionIdsForNewPlayers(),
                championInfo.freeChampionIds()
        );
    }
}
