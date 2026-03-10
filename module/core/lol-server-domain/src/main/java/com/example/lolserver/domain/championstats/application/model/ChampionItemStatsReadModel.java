package com.example.lolserver.domain.championstats.application.model;

public record ChampionItemStatsReadModel(
    int itemId,
    String itemName,
    long games,
    double winRate,
    double pickRate
) {}
