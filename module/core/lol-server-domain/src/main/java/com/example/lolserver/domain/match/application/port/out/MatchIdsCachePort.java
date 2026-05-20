package com.example.lolserver.domain.match.application.port.out;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface MatchIdsCachePort {

    /**
     * ZSET match:ids:v1:{puuid} 에서 puuid 의 최근 matchId 목록을 조회한다.
     * <p>
     * - seasonStartMs/seasonEndMs 가 모두 null → ZREVRANGE 0 -1 (전체)
     * - seasonStartMs/seasonEndMs 가 모두 값 → ZREVRANGEBYSCORE end start (시즌 범위)
     * <p>
     * 키 자체가 없으면 Optional.empty() 를 반환 (캐시 미스). 빈 ZSET 은 Optional.of(emptyList()) 로 구분한다.
     */
    Optional<List<String>> findIds(String puuid, Long seasonStartMs, Long seasonEndMs);

    /**
     * ZSET match:ids:v1:{puuid} 에 matchId-score (gameCreation epoch ms) 쌍을 pipeline 으로 저장한다.
     * 저장 후 ZREMRANGEBYRANK 0 -21 로 최신 20개만 유지하고 EXPIRE 86400 (24h) 를 갱신한다.
     */
    void saveIds(String puuid, List<Map.Entry<String, Long>> matchIdToScore);
}
