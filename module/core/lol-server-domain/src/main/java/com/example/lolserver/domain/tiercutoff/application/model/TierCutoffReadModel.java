package com.example.lolserver.domain.tiercutoff.application.model;

public record TierCutoffReadModel(
    Long id,
    String queue,
    String tier,
    String region,
    int minLeaguePoints,
    String updatedAt
) {}
