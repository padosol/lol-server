package com.example.lolserver.match.adapter.out.cache;

import com.example.lolserver.match.application.model.GameReadModel;
import com.example.lolserver.match.application.port.out.MatchSingleCachePort;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * lol-repository 가 Redis `match:v1:{matchId}` 에 {@link GameReadModel} 형태로 직렬화해 둔 JSON 을 읽는다.
 * 변환은 쓰기 측(lol-repository)에서 끝나므로 여기서는 raw JSON 을 그대로 역직렬화만 한다.
 * <p>
 * {@link GameReadModel} 트리의 값 객체 (ItemValue/StatValue/TeamData 등) 는 setter 가 없어
 * 필드 직접 접근 가시성을 켠 전용 {@link ObjectMapper} 를 사용한다.
 */
@Slf4j
@Component
public class MatchSingleCacheAdapter implements MatchSingleCachePort {

    static final String KEY_PREFIX = "match:v1:";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public MatchSingleCacheAdapter(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = JsonMapper.builder()
                .visibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .build();
    }

    @Override
    public Map<String, GameReadModel> findByIds(Collection<String> matchIds) {
        if (matchIds == null || matchIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<String> ids = new ArrayList<>(matchIds);
        List<String> keys = ids.stream().map(this::buildKey).toList();

        try {
            List<String> values = stringRedisTemplate.opsForValue().multiGet(keys);
            if (values == null) {
                return Collections.emptyMap();
            }

            Map<String, GameReadModel> result = new HashMap<>();
            for (int i = 0; i < ids.size(); i++) {
                String json = i < values.size() ? values.get(i) : null;
                if (json == null) {
                    continue;
                }
                GameReadModel game = deserialize(json);
                if (game != null) {
                    result.put(ids.get(i), game);
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("매치 단건 캐시 MGET 실패 - count: {}, message: {}", ids.size(), e.getMessage());
            return Collections.emptyMap();
        }
    }

    private GameReadModel deserialize(String json) {
        try {
            return objectMapper.readValue(json, GameReadModel.class);
        } catch (Exception e) {
            log.warn("매치 단건 캐시 역직렬화 실패 - message: {}", e.getMessage());
            return null;
        }
    }

    private String buildKey(String matchId) {
        return KEY_PREFIX + matchId;
    }
}
