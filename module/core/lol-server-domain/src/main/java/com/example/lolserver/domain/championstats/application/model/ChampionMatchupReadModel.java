package com.example.lolserver.domain.championstats.application.model;

public record ChampionMatchupReadModel(
    int opponentChampionId,
    long games,
    double winRate,
    double pickRate
) {}
