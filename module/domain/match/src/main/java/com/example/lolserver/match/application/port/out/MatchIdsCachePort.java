package com.example.lolserver.match.application.port.out;

import java.util.List;
import java.util.Optional;

public interface MatchIdsCachePort {

    /**
     * 유저의 최근 매치 ID 목록을 정렬 순(최신 → 과거)으로 반환한다.
     * 캐시 미스이면 {@code Optional.empty()} 를 반환한다.
     */
    Optional<List<String>> findIds(String puuid);
}
