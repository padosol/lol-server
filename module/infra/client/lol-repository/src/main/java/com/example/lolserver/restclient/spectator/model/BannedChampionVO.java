package com.example.lolserver.restclient.spectator.model;

/**
 * Riot API 밴 챔피언 VO
 */
public record BannedChampionVO(
    long championId,
    long teamId,
    int pickTurn
) {}
