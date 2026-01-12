package com.example.lolserver.restclient.summoner.model;

import java.util.List;

public record ChampionInfo(
        int maxNewPlayerLevel,
        List<Integer> freeChampionIdsForNewPlayers,
        List<Integer> freeChampionIds
) {
}
