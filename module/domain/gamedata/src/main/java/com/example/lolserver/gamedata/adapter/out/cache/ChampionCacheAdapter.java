package com.example.lolserver.gamedata.adapter.out.cache;

import com.example.lolserver.gamedata.application.port.out.ChampionPersistencePort;
import com.example.lolserver.gamedata.domain.ChampionRotate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
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
    private static final int SCAN_COUNT = 100;

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

    /**
     * platformId 는 요청마다 자유롭게 들어오므로 캐시에 어떤 플랫폼이 적재돼 있는지 미리 알 수 없다.
     * KEYS 대신 SCAN 으로 접두사 매칭 키를 모아 한 번에 삭제한다.
     */
    @Override
    public void evictChampionRotate() {
        ScanOptions options = ScanOptions.scanOptions()
                .match(CACHE_KEY_PREFIX + "*")
                .count(SCAN_COUNT)
                .build();

        List<String> keys = new ArrayList<>();
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                keys.add(cursor.next());
            }
        }

        if (keys.isEmpty()) {
            log.info("Champion rotation cache evict - 삭제 대상 키 없음");
            return;
        }

        Long deleted = redisTemplate.delete(keys);
        log.info("Champion rotation cache evicted - 대상: {}, 삭제: {}", keys.size(), deleted);
    }
}
