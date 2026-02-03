package com.example.lolserver.domain.spectator.application.port.out;

import com.example.lolserver.domain.spectator.application.model.CurrentGameInfoReadModel;

/**
 * Spectator 캐시 포트 - Redis 어댑터에서 구현
 */
public interface SpectatorCachePort {

    /**
     * puuid로 현재 진행 중인 게임 정보를 조회합니다.
     *
     * @param region 지역 (e.g., "kr")
     * @param puuid  소환사 puuid
     * @return 게임 정보 또는 null
     */
    CurrentGameInfoReadModel findByPuuid(String region, String puuid);

    /**
     * 현재 게임 정보를 저장합니다.
     * 모든 참여자의 puuid를 키로 저장하여 어떤 참여자로도 조회 가능하게 합니다.
     *
     * @param region   지역 (e.g., "kr")
     * @param gameInfo 저장할 게임 정보
     */
    void saveCurrentGame(String region, CurrentGameInfoReadModel gameInfo);

    /**
     * puuid로 캐시된 게임 정보를 삭제합니다.
     *
     * @param region 지역 (e.g., "kr")
     * @param puuid  소환사 puuid
     */
    void deleteByPuuid(String region, String puuid);

    // === Negative Caching ===

    /**
     * 게임 중이 아님을 캐싱합니다 (Negative Cache).
     * TTL 30초 동안 유지됩니다.
     *
     * @param region 지역 (e.g., "kr")
     * @param puuid  소환사 puuid
     */
    void saveNoGame(String region, String puuid);

    /**
     * Negative Cache가 존재하는지 확인합니다.
     *
     * @param region 지역 (e.g., "kr")
     * @param puuid  소환사 puuid
     * @return 게임 없음이 캐시되어 있으면 true
     */
    boolean isNoGameCached(String region, String puuid);

    // === 게임 메타데이터 (모든 참여자 삭제용) ===

    /**
     * 게임 메타데이터를 저장합니다.
     * 게임 무효화 시 모든 참여자 캐시를 삭제하기 위해 사용됩니다.
     *
     * @param region            지역 (e.g., "kr")
     * @param gameId            게임 ID
     * @param gameStartTime     게임 시작 시간 (Unix Timestamp)
     * @param participantPuuids 참여자 puuid 목록
     */
    void saveGameMeta(String region, long gameId, long gameStartTime, java.util.List<String> participantPuuids);

    /**
     * 게임 메타데이터를 조회하여 모든 참여자 캐시를 삭제합니다.
     *
     * @param region 지역 (e.g., "kr")
     * @param gameId 게임 ID
     */
    void deleteGameWithAllParticipants(String region, long gameId);
}
