package com.example.lolserver.domain.championstats.application.model;

public record ChampionRateReadModel(int championId, double winRate, double pickRate, double banRate) {}
