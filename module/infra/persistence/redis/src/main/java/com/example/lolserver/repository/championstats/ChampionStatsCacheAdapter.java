package com.example.lolserver.repository.championstats;

import com.example.lolserver.domain.championstats.application.model.ChampionStatsReadModel;
import com.example.lolserver.domain.championstats.application.model.PositionChampionStatsReadModel;
import com.example.lolserver.domain.championstats.application.port.out.ChampionStatsCachePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChampionStatsCacheAdapter implements ChampionStatsCachePort {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String DETAIL_KEY_PREFIX = "champion-stats:v4:detail:";
    private static final String POSITIONS_KEY_PREFIX = "champion-stats:v4:positions:";
    private static final Duration CACHE_TTL = Duration.ofHours(6);

    @Override
    public ChampionStatsReadModel findChampionStats(
            int championId, String patch, String platformId, String tierDisplay) {
        return tryGetFromCache(buildDetailKey(championId, patch, platformId, tierDisplay));
    }

    @Override
    public void saveChampionStats(
            int championId, String patch, String platformId,
            String tierDisplay, ChampionStatsReadModel stats) {
        if (stats == null) {
            return;
        }
        try {
            String key = buildDetailKey(championId, patch, platformId, tierDisplay);
            log.debug("캐시 저장 - key: {}", key);
            redisTemplate.opsForValue().set(key, stats, CACHE_TTL);
        } catch (Exception e) {
            log.warn("캐시 저장 실패 - championId: {}, patch: {}, tier: {}", championId, patch, tierDisplay, e);
        }
    }

    @Override
    public List<PositionChampionStatsReadModel> findChampionStatsByPosition(
            String patch, String platformId, String tierDisplay) {
        return tryGetFromCache(buildPositionsKey(patch, platformId, tierDisplay));
    }

    @Override
    public void saveChampionStatsByPosition(
            String patch, String platformId, String tierDisplay,
            List<PositionChampionStatsReadModel> stats) {
        if (stats == null) {
            return;
        }
        try {
            String key = buildPositionsKey(patch, platformId, tierDisplay);
            log.debug("캐시 저장 - key: {}", key);
            redisTemplate.opsForValue().set(key, stats, CACHE_TTL);
        } catch (Exception e) {
            log.warn("캐시 저장 실패 - positions, patch: {}, tier: {}", patch, tierDisplay, e);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T tryGetFromCache(String key) {
        try {
            log.debug("캐시 조회 - key: {}", key);
            return (T) redisTemplate.opsForValue().get(key);
        } catch (SerializationException e) {
            log.info("캐시 stale 감지 - 키 삭제 후 재생성: {}", key);
            evictQuietly(key);
            return null;
        } catch (Exception e) {
            log.debug("캐시 조회 실패 - key: {}, message: {}", key, e.getMessage());
            return null;
        }
    }

    private void evictQuietly(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception ignored) {
        }
    }

    private String buildDetailKey(int championId, String patch, String platformId, String tierDisplay) {
        return DETAIL_KEY_PREFIX + platformId + ":" + championId + ":" + patch + ":" + tierDisplay;
    }

    private String buildPositionsKey(String patch, String platformId, String tierDisplay) {
        return POSITIONS_KEY_PREFIX + platformId + ":" + patch + ":" + tierDisplay;
    }
}
