package com.example.lolserver.repository.match;

import com.example.lolserver.domain.match.application.model.GameReadModel;
import com.example.lolserver.domain.match.domain.gamedata.GameInfoData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class MatchSingleCacheAdapterTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private MatchSingleCacheAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new MatchSingleCacheAdapter(redisTemplate);
    }

    @DisplayName("findByIds 는 match:v1:{matchId} 키로 MGET 하여 hit 항목만 맵으로 반환한다")
    @Test
    void findByIds_returnsOnlyHitEntries() {
        // given
        List<String> ids = List.of("KR_1", "KR_2", "KR_3");
        List<String> expectedKeys = List.of("match:v1:KR_1", "match:v1:KR_2", "match:v1:KR_3");

        GameReadModel g1 = gameOf("KR_1");
        GameReadModel g3 = gameOf("KR_3");

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.multiGet(expectedKeys))
                .willReturn(Arrays.asList(g1, null, g3));

        // when
        Map<String, GameReadModel> result = adapter.findByIds(ids);

        // then
        assertThat(result).containsOnlyKeys("KR_1", "KR_3");
        assertThat(result.get("KR_1")).isSameAs(g1);
        assertThat(result.get("KR_3")).isSameAs(g3);
    }

    @DisplayName("findByIds 빈 입력은 곧바로 빈 맵을 반환한다")
    @Test
    void findByIds_emptyInput_returnsEmpty() {
        Map<String, GameReadModel> result = adapter.findByIds(Collections.emptyList());
        assertThat(result).isEmpty();
    }

    @DisplayName("findByIds Redis 장애 시 빈 맵을 반환한다")
    @Test
    void findByIds_redisFailure_returnsEmpty() {
        given(redisTemplate.opsForValue()).willThrow(new RuntimeException("Redis down"));

        Map<String, GameReadModel> result = adapter.findByIds(List.of("KR_1"));
        assertThat(result).isEmpty();
    }

    @DisplayName("saveAll 빈 입력은 Redis 와 상호작용하지 않는다")
    @Test
    void saveAll_emptyInput_noInteraction() {
        adapter.saveAll(Collections.emptyMap());
        // RedisTemplate 와의 어떤 상호작용도 없어야 한다 (executePipelined 호출되지 않음)
    }

    private GameReadModel gameOf(String matchId) {
        GameReadModel game = new GameReadModel();
        GameInfoData info = new GameInfoData();
        info.setMatchId(matchId);
        game.setGameInfoData(info);
        return game;
    }
}
