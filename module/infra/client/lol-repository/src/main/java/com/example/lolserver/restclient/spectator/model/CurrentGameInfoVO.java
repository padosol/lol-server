package com.example.lolserver.restclient.spectator.model;

import java.util.List;

/**
 * Riot API 현재 게임 정보 VO
 */
public record CurrentGameInfoVO(
    long gameId,
    String gameType,
    String gameMode,
    long mapId,
    long gameQueueConfigId,
    long gameStartTime,
    long gameLength,
    String platformId,
    ObserversVO observers,
    List<ParticipantVO> participants,
    List<BannedChampionVO> bannedChampions
) {}
