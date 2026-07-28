package com.example.lolserver.championstats.application.model.readmodel;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record ChampionRateReadModel(
        int championId,
        double winRate,
        double pickRate,
        double banRate,
        @JsonIgnore long totalGames,
        String tier
) {
    public ChampionRateReadModel(int championId, double winRate, double pickRate,
                                  double banRate, long totalGames) {
        this(championId, winRate, pickRate, banRate, totalGames, null);
    }

    public ChampionRateReadModel withTier(String tier) {
        return new ChampionRateReadModel(championId, winRate, pickRate, banRate, totalGames, tier);
    }
}
