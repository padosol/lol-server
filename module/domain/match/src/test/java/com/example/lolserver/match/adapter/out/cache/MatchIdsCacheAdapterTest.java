package com.example.lolserver.match.adapter.out.cache;

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

    @DisplayName("findIds 키 없음/빈 결과이면 Optional.empty 를 반환한다")
    @Test
    void findIds_missing_returnsEmptyOptional() {
        given(redisTemplate.opsForZSet()).willReturn(zSetOperations);
        given(zSetOperations.reverseRange("match:ids:v1:test-puuid", 0, -1)).willReturn(new LinkedHashSet<>());

        Optional<List<String>> result = adapter.findIds("test-puuid");

        assertThat(result).isEmpty();
    }

    @DisplayName("findIds 멤버가 있으면 최신순 리스트를 반환한다")
    @Test
    void findIds_populated_returnsIdsInOrder() {
        given(redisTemplate.opsForZSet()).willReturn(zSetOperations);

        Set<Object> raw = new LinkedHashSet<>();
        raw.add("KR_3");
        raw.add("KR_2");
        raw.add("KR_1");
        given(zSetOperations.reverseRange("match:ids:v1:test-puuid", 0, -1)).willReturn(raw);

        Optional<List<String>> result = adapter.findIds("test-puuid");

        assertThat(result).isPresent();
        assertThat(result.get()).containsExactly("KR_3", "KR_2", "KR_1");
    }

}
