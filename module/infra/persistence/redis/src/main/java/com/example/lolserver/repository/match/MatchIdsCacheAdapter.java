package com.example.lolserver.repository.match;

import com.example.lolserver.domain.match.application.port.out.MatchIdsCachePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class MatchIdsCacheAdapter implements MatchIdsCachePort {

    static final String KEY_PREFIX = "match:ids:v1:";
    static final int CAP = 20;
    private static final Duration CACHE_TTL = Duration.ofHours(24);
    private static final long CACHE_TTL_SECONDS = CACHE_TTL.toSeconds();

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public Optional<List<String>> findIds(String puuid, Long seasonStartMs, Long seasonEndMs) {
        String key = buildKey(puuid);
        try {
            Boolean exists = redisTemplate.hasKey(key);
            if (Boolean.FALSE.equals(exists)) {
                return Optional.empty();
            }

            ZSetOperations<String, Object> ops = redisTemplate.opsForZSet();
            Set<Object> raw;
            if (seasonStartMs == null && seasonEndMs == null) {
                raw = ops.reverseRange(key, 0, -1);
            } else if (seasonStartMs != null && seasonEndMs != null) {
                raw = ops.reverseRangeByScore(key, seasonStartMs, seasonEndMs);
            } else {
                // 한 쪽만 값이 있으면 contract 위반 — 보수적으로 전체 범위 조회
                raw = ops.reverseRange(key, 0, -1);
            }

            if (raw == null) {
                return Optional.of(Collections.emptyList());
            }

            List<String> ids = new ArrayList<>(raw.size());
            for (Object o : raw) {
                if (o instanceof String s) {
                    ids.add(s);
                } else if (o != null) {
                    ids.add(o.toString());
                }
            }
            return Optional.of(ids);
        } catch (Exception e) {
            log.warn("매치 ID ZSET 조회 실패 - puuid: {}, message: {}", puuid, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void saveIds(String puuid, List<Map.Entry<String, Long>> matchIdToScore) {
        if (matchIdToScore == null || matchIdToScore.isEmpty()) {
            return;
        }

        String key = buildKey(puuid);
        try {
            RedisSerializer<String> keySerializer = redisTemplate.getStringSerializer();
            @SuppressWarnings("unchecked")
            RedisSerializer<Object> valueSerializer = (RedisSerializer<Object>) redisTemplate.getValueSerializer();

            byte[] rawKey = keySerializer.serialize(key);
            if (rawKey == null) {
                return;
            }

            redisTemplate.executePipelined((RedisConnection connection) -> {
                for (Map.Entry<String, Long> entry : matchIdToScore) {
                    if (entry.getKey() == null || entry.getValue() == null) {
                        continue;
                    }
                    byte[] member = valueSerializer.serialize(entry.getKey());
                    if (member == null) {
                        continue;
                    }
                    connection.zSetCommands().zAdd(rawKey, entry.getValue(), member);
                }
                // 최신 CAP 개만 유지: 오래된 인덱스 0 ~ -(CAP+1) 제거
                connection.zSetCommands().zRemRange(rawKey, 0, -(CAP + 1L));
                connection.keyCommands().expire(rawKey, CACHE_TTL_SECONDS);
                return null;
            });
        } catch (Exception e) {
            log.warn("매치 ID ZSET 저장 실패 - puuid: {}, message: {}", puuid, e.getMessage());
        }
    }

    private String buildKey(String puuid) {
        return KEY_PREFIX + puuid;
    }
}
