package com.example.lolserver.repository.match;

import com.example.lolserver.domain.match.application.model.GameReadModel;
import com.example.lolserver.domain.match.application.port.out.MatchSingleCachePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class MatchSingleCacheAdapter implements MatchSingleCachePort {

    static final String KEY_PREFIX = "match:v1:";
    private static final Duration CACHE_TTL = Duration.ofHours(1);
    private static final long CACHE_TTL_SECONDS = CACHE_TTL.toSeconds();

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public Map<String, GameReadModel> findByIds(Collection<String> matchIds) {
        if (matchIds == null || matchIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<String> ids = new ArrayList<>(matchIds);
        List<String> keys = ids.stream().map(this::buildKey).toList();

        try {
            List<Object> values = redisTemplate.opsForValue().multiGet(keys);
            if (values == null) {
                return Collections.emptyMap();
            }

            Map<String, GameReadModel> result = new HashMap<>();
            for (int i = 0; i < ids.size(); i++) {
                Object raw = i < values.size() ? values.get(i) : null;
                if (raw instanceof GameReadModel game) {
                    result.put(ids.get(i), game);
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("매치 단건 캐시 MGET 실패 - count: {}, message: {}", ids.size(), e.getMessage());
            return Collections.emptyMap();
        }
    }

    @Override
    public void saveAll(Map<String, GameReadModel> matches) {
        if (matches == null || matches.isEmpty()) {
            return;
        }

        try {
            RedisSerializer<String> keySerializer = redisTemplate.getStringSerializer();
            @SuppressWarnings("unchecked")
            RedisSerializer<Object> valueSerializer = (RedisSerializer<Object>) redisTemplate.getValueSerializer();

            redisTemplate.executePipelined((RedisConnection connection) -> {
                for (Map.Entry<String, GameReadModel> entry : matches.entrySet()) {
                    if (entry.getKey() == null || entry.getValue() == null) {
                        continue;
                    }
                    byte[] rawKey = keySerializer.serialize(buildKey(entry.getKey()));
                    byte[] rawValue = valueSerializer.serialize(entry.getValue());
                    if (rawKey == null || rawValue == null) {
                        continue;
                    }
                    connection.stringCommands().setEx(rawKey, CACHE_TTL_SECONDS, rawValue);
                }
                return null;
            });
        } catch (Exception e) {
            log.warn("매치 단건 캐시 pipeline 저장 실패 - count: {}, message: {}", matches.size(), e.getMessage());
        }
    }

    private String buildKey(String matchId) {
        return KEY_PREFIX + matchId;
    }
}
