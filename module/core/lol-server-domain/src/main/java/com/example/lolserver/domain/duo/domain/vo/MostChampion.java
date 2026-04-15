package com.example.lolserver.domain.duo.domain.vo;

public record MostChampion(
        int championId,
        String championName,
        long playCount,
        long wins,
        long losses
) {
}
