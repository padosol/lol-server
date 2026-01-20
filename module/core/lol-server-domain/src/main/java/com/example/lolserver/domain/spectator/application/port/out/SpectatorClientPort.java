package com.example.lolserver.domain.spectator.application.port.out;

import com.example.lolserver.domain.spectator.application.model.CurrentGameInfoReadModel;

/**
 * Spectator 클라이언트 포트 - Riot API 클라이언트에서 구현
 */
public interface SpectatorClientPort {

    /**
     * Riot API에서 현재 진행 중인 게임 정보를 조회합니다.
     *
     * @param region 지역 (e.g., "kr")
     * @param puuid  소환사 puuid
     * @return 게임 정보 또는 null (게임 중이 아닌 경우)
     */
    CurrentGameInfoReadModel getCurrentGameInfo(String region, String puuid);
}
