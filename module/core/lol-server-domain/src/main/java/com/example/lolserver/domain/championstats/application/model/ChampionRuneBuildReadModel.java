package com.example.lolserver.domain.championstats.application.model;

public record ChampionRuneBuildReadModel(
    int primaryStyleId,
    int subStyleId,
    int primaryPerk0,
    int primaryPerk1,
    int primaryPerk2,
    int primaryPerk3,
    int subPerk0,
    int subPerk1,
    long games,
    double winRate,
    double pickRate
) {}
