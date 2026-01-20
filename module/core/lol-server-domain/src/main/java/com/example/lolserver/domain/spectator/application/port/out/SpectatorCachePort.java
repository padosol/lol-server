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
     * 추후 구현 예정
     *
     * @param region 지역 (e.g., "kr")
     * @param puuid  소환사 puuid
     */
    void deleteByPuuid(String region, String puuid);
}
