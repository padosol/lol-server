package com.example.lolserver.match.adapter.out.cache;

import com.example.lolserver.match.application.port.out.MatchIdsCachePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class MatchIdsCacheAdapter implements MatchIdsCachePort {

    static final String KEY_PREFIX = "match:ids:v1:";

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public Optional<List<String>> findIds(String puuid) {
        String key = buildKey(puuid);
        try {
            Set<Object> raw = redisTemplate.opsForZSet().reverseRange(key, 0, -1);
            if (raw == null || raw.isEmpty()) {
                return Optional.empty();
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

    private String buildKey(String puuid) {
        return KEY_PREFIX + puuid;
    }
}
