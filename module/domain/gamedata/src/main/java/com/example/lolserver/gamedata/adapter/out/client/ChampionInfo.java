package com.example.lolserver.gamedata.adapter.out.client;

import java.util.List;

public record ChampionInfo(
        int maxNewPlayerLevel,
        List<Integer> freeChampionIdsForNewPlayers,
        List<Integer> freeChampionIds
) {
}
