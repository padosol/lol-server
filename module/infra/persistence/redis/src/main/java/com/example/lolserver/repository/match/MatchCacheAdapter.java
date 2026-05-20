package com.example.lolserver.repository.match;

import com.example.lolserver.domain.match.application.model.GameReadModel;
import com.example.lolserver.domain.match.application.port.out.MatchCachePort;
import com.example.lolserver.support.SliceResult;
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
public class MatchCacheAdapter implements MatchCachePort {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String KEY_PREFIX = "match:list:v1:";
    private static final String NULL_TOKEN = "_";
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    @Override
    public SliceResult<GameReadModel> findMatchesBatch(
            String puuid, Integer season, Integer queueId, Integer pageNo) {
        String key = buildKey(puuid, season, queueId, pageNo);
        try {
            log.debug("매치 목록 캐시 조회 - key: {}", key);
            Object raw = redisTemplate.opsForValue().get(key);
            if (raw == null) {
                return null;
            }
            if (raw instanceof CachedSlice cached) {
                return new SliceResult<>(cached.content(), cached.hasNext());
            }
            log.info("매치 목록 캐시 stale 타입 감지 - 키 삭제: {}", key);
            evictQuietly(key);
            return null;
        } catch (SerializationException e) {
            log.info("매치 목록 캐시 stale 감지 - 키 삭제 후 재생성: {}", key);
            evictQuietly(key);
            return null;
        } catch (Exception e) {
            log.debug("매치 목록 캐시 조회 실패 - key: {}, message: {}", key, e.getMessage());
            return null;
        }
    }

    @Override
    public void saveMatchesBatch(
            String puuid, Integer season, Integer queueId, Integer pageNo,
            SliceResult<GameReadModel> matches) {
        if (matches == null) {
            return;
        }
        String key = buildKey(puuid, season, queueId, pageNo);
        try {
            log.debug("매치 목록 캐시 저장 - key: {}", key);
            CachedSlice payload = new CachedSlice(matches.getContent(), matches.isHasNext());
            redisTemplate.opsForValue().set(key, payload, CACHE_TTL);
        } catch (Exception e) {
            log.warn("매치 목록 캐시 저장 실패 - puuid: {}, season: {}, queueId: {}, pageNo: {}",
                    puuid, season, queueId, pageNo, e);
        }
    }

    private void evictQuietly(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception ignored) {
        }
    }

    private String buildKey(String puuid, Integer season, Integer queueId, Integer pageNo) {
        return KEY_PREFIX + puuid
                + ":" + (season == null ? NULL_TOKEN : season)
                + ":" + (queueId == null ? NULL_TOKEN : queueId)
                + ":" + pageNo;
    }

    public record CachedSlice(List<GameReadModel> content, boolean hasNext) {
    }
}
