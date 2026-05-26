package com.example.lolserver.gamedata.application.model;

public record TierCutoffReadModel(
    Long id,
    String queue,
    String tier,
    String platformId,
    int minLeaguePoints,
    int lpChange,
    int userCount,
    String updatedAt
) {}
