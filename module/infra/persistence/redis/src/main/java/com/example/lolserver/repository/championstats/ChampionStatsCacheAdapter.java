package com.example.lolserver.repository.championstats;

import com.example.lolserver.domain.championstats.application.model.ChampionStatsReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionTimelineReadModel;
import com.example.lolserver.domain.championstats.application.model.PositionChampionStatsReadModel;
import com.example.lolserver.domain.championstats.application.port.out.ChampionStatsCachePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChampionStatsCacheAdapter implements ChampionStatsCachePort {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedissonClient redissonClient;

    private static final String DETAIL_KEY_PREFIX = "champion-stats:v7:detail:";
    private static final String POSITIONS_KEY_PREFIX = "champion-stats:v4:positions:";
    private static final String TIMELINE_KEY_PREFIX = "champion-stats:v1:timeline:";
    private static final String DETAIL_LOCK_PREFIX = "champion-stats:lock:detail:";
    private static final String POSITIONS_LOCK_PREFIX = "champion-stats:lock:positions:";
    private static final String TIMELINE_LOCK_PREFIX = "champion-stats:lock:timeline:";
    private static final Duration CACHE_TTL = Duration.ofHours(6);
    private static final long LOCK_WAIT_TIME_SECONDS = 3L;
    private static final long LOCK_LEASE_TIME_SECONDS = 30L;

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

    @Override
    public ChampionTimelineReadModel findChampionTimeline(
            int championId, String patch, String platformId, String tierDisplay) {
        return tryGetFromCache(buildTimelineKey(championId, patch, platformId, tierDisplay));
    }

    @Override
    public void saveChampionTimeline(int championId, String patch, String platformId,
                                     String tierDisplay, ChampionTimelineReadModel timeline) {
        if (timeline == null) {
            return;
        }
        try {
            String key = buildTimelineKey(championId, patch, platformId, tierDisplay);
            log.debug("캐시 저장 - key: {}", key);
            redisTemplate.opsForValue().set(key, timeline, CACHE_TTL);
        } catch (Exception e) {
            log.warn("캐시 저장 실패 - timeline championId: {}, patch: {}, tier: {}",
                    championId, patch, tierDisplay, e);
        }
    }

    @Override
    public boolean tryLockDetail(int championId, String patch, String platformId, String tierDisplay) {
        return tryLock(buildDetailLockKey(championId, patch, platformId, tierDisplay));
    }

    @Override
    public void unlockDetail(int championId, String patch, String platformId, String tierDisplay) {
        unlock(buildDetailLockKey(championId, patch, platformId, tierDisplay));
    }

    @Override
    public boolean tryLockByPosition(String patch, String platformId, String tierDisplay) {
        return tryLock(buildPositionsLockKey(patch, platformId, tierDisplay));
    }

    @Override
    public void unlockByPosition(String patch, String platformId, String tierDisplay) {
        unlock(buildPositionsLockKey(patch, platformId, tierDisplay));
    }

    @Override
    public boolean tryLockTimeline(int championId, String patch, String platformId, String tierDisplay) {
        return tryLock(buildTimelineLockKey(championId, patch, platformId, tierDisplay));
    }

    @Override
    public void unlockTimeline(int championId, String patch, String platformId, String tierDisplay) {
        unlock(buildTimelineLockKey(championId, patch, platformId, tierDisplay));
    }

    private boolean tryLock(String lockKey) {
        RLock lock = redissonClient.getLock(lockKey);
        try {
            return lock.tryLock(LOCK_WAIT_TIME_SECONDS, LOCK_LEASE_TIME_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Lock acquisition interrupted - key: {}", lockKey, e);
            return false;
        }
    }

    private void unlock(String lockKey) {
        try {
            RLock lock = redissonClient.getLock(lockKey);
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        } catch (Exception e) {
            log.warn("Lock release failed - key: {}", lockKey, e);
        }
    }

    private String buildDetailKey(int championId, String patch, String platformId, String tierDisplay) {
        return DETAIL_KEY_PREFIX + platformId + ":" + championId + ":" + patch + ":" + tierDisplay;
    }

    private String buildPositionsKey(String patch, String platformId, String tierDisplay) {
        return POSITIONS_KEY_PREFIX + platformId + ":" + patch + ":" + tierDisplay;
    }

    private String buildTimelineKey(int championId, String patch, String platformId, String tierDisplay) {
        return TIMELINE_KEY_PREFIX + platformId + ":" + championId + ":" + patch + ":" + tierDisplay;
    }

    private String buildDetailLockKey(int championId, String patch, String platformId, String tierDisplay) {
        return DETAIL_LOCK_PREFIX + platformId + ":" + championId + ":" + patch + ":" + tierDisplay;
    }

    private String buildPositionsLockKey(String patch, String platformId, String tierDisplay) {
        return POSITIONS_LOCK_PREFIX + platformId + ":" + patch + ":" + tierDisplay;
    }

    private String buildTimelineLockKey(int championId, String patch, String platformId, String tierDisplay) {
        return TIMELINE_LOCK_PREFIX + platformId + ":" + championId + ":" + patch + ":" + tierDisplay;
    }
}
