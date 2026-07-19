package com.example.lolserver.gamedata.adapter.out.cache;

import com.example.lolserver.gamedata.application.port.out.ChampionPersistencePort;
import com.example.lolserver.gamedata.domain.ChampionRotate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChampionCacheAdapter implements ChampionPersistencePort {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String CACHE_KEY_PREFIX = "champion_rotation_";
    private static final Duration CACHE_TTL = Duration.ofHours(1); // 정상 로테이션 캐시 (1시간)
    // 빈 로테이션(upstream/Riot 조회 실패)은 짧게만 캐싱한다. 아예 캐싱하지 않으면 매 요청이
    // upstream→Riot 으로 관통해 rate limit 을 소모하고, 1시간을 캐싱하면 upstream 이 복구된 뒤에도
    // 빈 값이 고착된다. 짧은 TTL 의 negative cache 로 두 문제를 동시에 방지한다.
    private static final Duration EMPTY_CACHE_TTL = Duration.ofSeconds(60);

    @Override
    public Optional<ChampionRotate> getChampionRotate(String platformId) {
        log.info("Attempting to retrieve champion rotation from cache for platformId: {}", platformId);
        return Optional.ofNullable((ChampionRotate) redisTemplate.opsForValue().get(CACHE_KEY_PREFIX + platformId));
    }

    @Override
    public void saveChampionRotate(String platformId, ChampionRotate championRotate) {
        Duration ttl = championRotate.isEmpty() ? EMPTY_CACHE_TTL : CACHE_TTL;
        log.info("Caching champion rotation for platformId: {} (empty={}, ttl={})",
                platformId, championRotate.isEmpty(), ttl);
        redisTemplate.opsForValue().set(CACHE_KEY_PREFIX + platformId, championRotate, ttl);
    }
}
