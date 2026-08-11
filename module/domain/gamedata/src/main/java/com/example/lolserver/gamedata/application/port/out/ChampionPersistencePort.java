package com.example.lolserver.gamedata.application.port.out;

import com.example.lolserver.gamedata.domain.ChampionRotate;
import java.util.Optional;

public interface ChampionPersistencePort {
    Optional<ChampionRotate> getChampionRotate(String platformId);
    void saveChampionRotate(String platformId, ChampionRotate championRotate);

    /**
     * 캐시된 모든 플랫폼의 챔피언 로테이션을 제거한다.
     * 로테이션은 매주 화요일 갱신되므로, 주간 스케줄러가 이 시점에 캐시를 비운다.
     */
    void evictChampionRotate();
}
