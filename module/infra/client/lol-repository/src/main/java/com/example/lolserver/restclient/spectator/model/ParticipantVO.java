package com.example.lolserver.restclient.spectator.model;

import java.util.List;

/**
 * Riot API 게임 참여자 VO
 */
public record ParticipantVO(
    String riotId,
    String puuid,
    long championId,
    long teamId,
    long spell1Id,
    long spell2Id,
    long profileIconId,
    boolean bot,
    List<GameCustomizationObjectVO> gameCustomizationObjects,
    PerksVO perks
) {}
