package com.example.lolserver.repository.match;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class MatchIdsCacheAdapterTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ZSetOperations<String, Object> zSetOperations;

    private MatchIdsCacheAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new MatchIdsCacheAdapter(redisTemplate);
    }

    @DisplayName("findIds 키 없음이면 Optional.empty 를 반환한다")
    @Test
    void findIds_keyMissing_returnsEmptyOptional() {
        given(redisTemplate.hasKey("match:ids:v1:test-puuid")).willReturn(false);

        Optional<List<String>> result = adapter.findIds("test-puuid", null, null);

        assertThat(result).isEmpty();
    }

    @DisplayName("findIds start/end 둘 다 null 이면 ZREVRANGE 0 -1 을 호출한다")
    @Test
    void findIds_nullRange_callsReverseRangeAll() {
        given(redisTemplate.hasKey("match:ids:v1:test-puuid")).willReturn(true);
        given(redisTemplate.opsForZSet()).willReturn(zSetOperations);

        Set<Object> raw = new LinkedHashSet<>();
        raw.add("KR_3");
        raw.add("KR_2");
        raw.add("KR_1");
        given(zSetOperations.reverseRange("match:ids:v1:test-puuid", 0, -1)).willReturn(raw);

        Optional<List<String>> result = adapter.findIds("test-puuid", null, null);

        assertThat(result).isPresent();
        assertThat(result.get()).containsExactly("KR_3", "KR_2", "KR_1");
    }

    @DisplayName("findIds start/end 모두 값이면 ZREVRANGEBYSCORE 를 호출한다")
    @Test
    void findIds_rangeProvided_callsReverseRangeByScore() {
        given(redisTemplate.hasKey("match:ids:v1:test-puuid")).willReturn(true);
        given(redisTemplate.opsForZSet()).willReturn(zSetOperations);

        Set<Object> raw = new LinkedHashSet<>();
        raw.add("KR_2");
        given(zSetOperations.reverseRangeByScore("match:ids:v1:test-puuid", 1000d, 2000d)).willReturn(raw);

        Optional<List<String>> result = adapter.findIds("test-puuid", 1000L, 2000L);

        assertThat(result).isPresent();
        assertThat(result.get()).containsExactly("KR_2");
    }

    @DisplayName("findIds 빈 ZSET 이면 Optional.of(emptyList) 를 반환한다")
    @Test
    void findIds_emptySet_returnsEmptyList() {
        given(redisTemplate.hasKey("match:ids:v1:test-puuid")).willReturn(true);
        given(redisTemplate.opsForZSet()).willReturn(zSetOperations);
        given(zSetOperations.reverseRange("match:ids:v1:test-puuid", 0, -1)).willReturn(new LinkedHashSet<>());

        Optional<List<String>> result = adapter.findIds("test-puuid", null, null);

        assertThat(result).isPresent();
        assertThat(result.get()).isEmpty();
    }

    @DisplayName("saveIds 빈 입력은 Redis 와 상호작용하지 않는다")
    @Test
    void saveIds_emptyInput_noInteraction() {
        adapter.saveIds("test-puuid", List.of());
        // RedisTemplate.executePipelined 호출 없음
    }
}
