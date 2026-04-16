package com.example.lolserver.domain.duo.domain.vo;

import java.util.List;

public record RecentGameSummary(
        int wins,
        int losses,
        List<PlayedChampion> playedChampions
) {

    public record PlayedChampion(int championId, String championName) {
    }
}
