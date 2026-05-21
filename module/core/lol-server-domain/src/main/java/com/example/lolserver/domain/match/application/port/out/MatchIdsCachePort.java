package com.example.lolserver.domain.match.application.port.out;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface MatchIdsCachePort {

    /**
     * 유저의 최근 매치 ID 목록을 정렬 순(최신 → 과거)으로 반환한다.
     * 캐시 미스이면 {@code Optional.empty()} 를 반환한다.
     */
    Optional<List<String>> findIds(String puuid);

    /**
     * 매치 ID 와 정렬 기준 (gameCreation epoch ms) 쌍을 캐시에 저장한다.
     * 캐시는 최신 N 개만 유지하며 일정 시간 후 만료된다.
     */
    void saveIds(String puuid, List<Map.Entry<String, Long>> matchIdToScore);
}
