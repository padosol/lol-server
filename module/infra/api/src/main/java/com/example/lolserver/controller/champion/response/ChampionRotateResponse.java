package com.example.lolserver.controller.champion.response;

import com.example.lolserver.domain.champion.domain.ChampionRotate;

import java.util.List;

public record ChampionRotateResponse(
        int maxNewPlayerLevel,
        List<Integer>freeChampionIdsForNewPlayers,
        List<Integer> freeChampionIds
) {

    public static ChampionRotateResponse of(ChampionRotate championRotate) {
        return new ChampionRotateResponse(
                championRotate.getMaxNewPlayerLevel(),
                championRotate.getFreeChampionIdsForNewPlayers(),
                championRotate.getFreeChampionIds()
        );
    }
}
