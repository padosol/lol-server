package com.example.lolserver.domain.spectator.model;

/**
 * 밴 된 챔피언 정보 ReadModel
 */
public record BannedChampionReadModel(
    long championId, // 밴 된 챔피언 ID
    long teamId, // 밴 한 팀 ID
    int pickTurn // 밴 순서
) {}
