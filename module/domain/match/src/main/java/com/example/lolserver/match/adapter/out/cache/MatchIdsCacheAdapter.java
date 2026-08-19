package com.example.lolserver.match.adapter.out.cache;

import com.example.lolserver.match.application.port.out.MatchIdsCachePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * lol-repository 가 Redis `match:ids:v1:{puuid}` ZSET (score = gameCreation) 에 Redisson StringCodec 으로
 * 써 둔 matchId 를 최신순으로 읽는다.
 * <p>
 * 멤버는 따옴표 없는 raw 문자열이라 JSON 직렬화를 쓰는 {@code RedisTemplate<String, Object>} 로는
 * 역직렬화가 실패하고 그 실패가 캐시 미스와 구분되지 않는다. {@link MatchSingleCacheAdapter} 와 같은 이유로
 * {@link StringRedisTemplate} 을 쓴다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MatchIdsCacheAdapter implements MatchIdsCachePort {

    static final String KEY_PREFIX = "match:ids:v1:";

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public Optional<List<String>> findIds(String puuid) {
        String key = buildKey(puuid);
        try {
            Set<String> raw = stringRedisTemplate.opsForZSet().reverseRange(key, 0, -1);
            if (raw == null || raw.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new ArrayList<>(raw));
        } catch (Exception e) {
            log.warn("매치 ID ZSET 조회 실패 - puuid: {}, message: {}", puuid, e.getMessage());
            return Optional.empty();
        }
    }

    private String buildKey(String puuid) {
        return KEY_PREFIX + puuid;
    }
}
