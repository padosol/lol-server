package com.example.lolserver.domain.spectator.application.model;

/**
 * 게임 참여자 정보 ReadModel
 */
public record ParticipantReadModel(
    String summonerName, // 소환사 이름
    String summonerId, // 소환사 ID
    long championId, // 챔피언 ID
    long teamId, // 팀 ID (100: 블루팀, 200: 레드팀)
    long spell1Id, // 소환사 주문 1 ID
    long spell2Id, // 소환사 주문 2 ID
    boolean isBot, // 봇 여부
    PerksReadModel perks // 룬 정보
) {}
