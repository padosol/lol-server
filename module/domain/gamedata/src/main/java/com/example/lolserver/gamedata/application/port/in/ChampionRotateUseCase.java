package com.example.lolserver.gamedata.application.port.in;

import com.example.lolserver.gamedata.domain.ChampionRotate;

public interface ChampionRotateUseCase {
    ChampionRotate getChampionRotate(String platformId);

    /**
     * 챔피언 로테이션 캐시를 전 플랫폼 제거한다.
     * 로테이션은 매주 화요일 갱신되므로 주간 스케줄러가 호출한다.
     */
    void evictChampionRotate();
}
