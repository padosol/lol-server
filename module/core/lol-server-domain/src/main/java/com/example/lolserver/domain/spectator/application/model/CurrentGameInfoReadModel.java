package com.example.lolserver.domain.spectator.application.model;

import java.util.List;

/**
 * 현재 진행중인 게임 정보 ReadModel
 */
public record CurrentGameInfoReadModel(
    long gameId, // 게임 ID
    String gameType, // 게임 타입 (e.g., "MATCHED_GAME")
    String gameMode, // 게임 모드 (e.g., "CLASSIC")
    long mapId, // 맵 ID
    long gameQueueConfigId, // 게임 큐 설정 ID
    long gameStartTime, // 게임 시작 시간 (Unix Timestamp)
    long gameLength, // 게임 진행 시간 (초)
    String platformId, // 플랫폼 ID (e.g., "KR")
    String encryptionKey, // 관전 암호화 키
    List<ParticipantReadModel> participants, // 참여자 목록
    List<BannedChampionReadModel> bannedChampions // 밴 된 챔피언 목록
) {}
