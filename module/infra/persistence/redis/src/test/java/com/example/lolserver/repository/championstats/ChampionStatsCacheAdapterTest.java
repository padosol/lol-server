package com.example.lolserver.repository.championstats;

import com.example.lolserver.domain.championstats.application.model.ChampionStatsReadModel;
import com.example.lolserver.domain.championstats.application.model.PositionChampionStatsReadModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class ChampionStatsCacheAdapterTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private ChampionStatsCacheAdapter adapter;

    private static final Duration CACHE_TTL = Duration.ofHours(6);

    @BeforeEach
    void setUp() {
        adapter = new ChampionStatsCacheAdapter(redisTemplate);
    }

    @DisplayName("챔피언 상세 통계 캐시 히트 시 데이터를 반환한다")
    @Test
    void findChampionStats_cacheHit_returnsData() {
        // given
        int championId = 13;
        String patch = "16.1";
        String platformId = "KR";
        String tierDisplay = "EMERALD";
        String expectedKey = "champion-stats:detail:KR:13:16.1:EMERALD";

        ChampionStatsReadModel cached = new ChampionStatsReadModel("EMERALD", List.of());

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(expectedKey)).willReturn(cached);

        // when
        ChampionStatsReadModel result = adapter.findChampionStats(championId, patch, platformId, tierDisplay);

        // then
        assertThat(result).isEqualTo(cached);
        then(valueOperations).should().get(expectedKey);
    }

    @DisplayName("챔피언 상세 통계 캐시 미스 시 null을 반환한다")
    @Test
    void findChampionStats_cacheMiss_returnsNull() {
        // given
        int championId = 13;
        String patch = "16.1";
        String platformId = "KR";
        String tierDisplay = "EMERALD";
        String expectedKey = "champion-stats:detail:KR:13:16.1:EMERALD";

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(expectedKey)).willReturn(null);

        // when
        ChampionStatsReadModel result = adapter.findChampionStats(championId, patch, platformId, tierDisplay);

        // then
        assertThat(result).isNull();
    }

    @DisplayName("챔피언 상세 통계를 캐시에 저장한다")
    @Test
    void saveChampionStats_validData_savesToCache() {
        // given
        int championId = 13;
        String patch = "16.1";
        String platformId = "KR";
        String tierDisplay = "EMERALD";
        String expectedKey = "champion-stats:detail:KR:13:16.1:EMERALD";

        ChampionStatsReadModel stats = new ChampionStatsReadModel("EMERALD", List.of());

        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        // when
        adapter.saveChampionStats(championId, patch, platformId, tierDisplay, stats);

        // then
        then(valueOperations).should().set(eq(expectedKey), eq(stats), eq(CACHE_TTL));
    }

    @DisplayName("null 통계는 캐시에 저장하지 않는다")
    @Test
    void saveChampionStats_nullData_doesNotSave() {
        // when
        adapter.saveChampionStats(13, "16.1", "KR", "EMERALD", null);

        // then
        then(redisTemplate).shouldHaveNoInteractions();
    }

    @DisplayName("포지션별 통계 캐시 히트 시 데이터를 반환한다")
    @Test
    void findChampionStatsByPosition_cacheHit_returnsData() {
        // given
        String patch = "16.1";
        String platformId = "KR";
        String tierDisplay = "EMERALD";
        String expectedKey = "champion-stats:positions:KR:16.1:EMERALD";

        List<PositionChampionStatsReadModel> cached = List.of(
            new PositionChampionStatsReadModel("TOP", List.of())
        );

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(expectedKey)).willReturn(cached);

        // when
        List<PositionChampionStatsReadModel> result = adapter.findChampionStatsByPosition(patch, platformId, tierDisplay);

        // then
        assertThat(result).isEqualTo(cached);
    }

    @DisplayName("포지션별 통계 캐시 미스 시 null을 반환한다")
    @Test
    void findChampionStatsByPosition_cacheMiss_returnsNull() {
        // given
        String patch = "16.1";
        String platformId = "KR";
        String tierDisplay = "EMERALD";
        String expectedKey = "champion-stats:positions:KR:16.1:EMERALD";

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(expectedKey)).willReturn(null);

        // when
        List<PositionChampionStatsReadModel> result = adapter.findChampionStatsByPosition(patch, platformId, tierDisplay);

        // then
        assertThat(result).isNull();
    }

    @DisplayName("포지션별 통계를 캐시에 저장한다")
    @Test
    void saveChampionStatsByPosition_validData_savesToCache() {
        // given
        String patch = "16.1";
        String platformId = "KR";
        String tierDisplay = "EMERALD";
        String expectedKey = "champion-stats:positions:KR:16.1:EMERALD";

        List<PositionChampionStatsReadModel> stats = List.of(
            new PositionChampionStatsReadModel("TOP", List.of())
        );

        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        // when
        adapter.saveChampionStatsByPosition(patch, platformId, tierDisplay, stats);

        // then
        then(valueOperations).should().set(eq(expectedKey), eq(stats), eq(CACHE_TTL));
    }

    @DisplayName("null 포지션별 통계는 캐시에 저장하지 않는다")
    @Test
    void saveChampionStatsByPosition_nullData_doesNotSave() {
        // when
        adapter.saveChampionStatsByPosition("16.1", "KR", "EMERALD", null);

        // then
        then(redisTemplate).shouldHaveNoInteractions();
    }

    @DisplayName("Redis 장애 시 캐시 조회는 null을 반환한다")
    @Test
    void findChampionStats_redisFailure_returnsNull() {
        // given
        given(redisTemplate.opsForValue()).willThrow(new RuntimeException("Redis connection failed"));

        // when
        ChampionStatsReadModel result = adapter.findChampionStats(13, "16.1", "KR", "EMERALD");

        // then
        assertThat(result).isNull();
    }

    @DisplayName("Redis 장애 시 캐시 저장은 예외를 전파하지 않는다")
    @Test
    void saveChampionStats_redisFailure_doesNotThrow() {
        // given
        ChampionStatsReadModel stats = new ChampionStatsReadModel("EMERALD", List.of());
        given(redisTemplate.opsForValue()).willThrow(new RuntimeException("Redis connection failed"));

        // when & then
        adapter.saveChampionStats(13, "16.1", "KR", "EMERALD", stats);
    }

    @DisplayName("Redis 장애 시 포지션별 캐시 조회는 null을 반환한다")
    @Test
    void findChampionStatsByPosition_redisFailure_returnsNull() {
        // given
        given(redisTemplate.opsForValue()).willThrow(new RuntimeException("Redis connection failed"));

        // when
        List<PositionChampionStatsReadModel> result = adapter.findChampionStatsByPosition("16.1", "KR", "EMERALD");

        // then
        assertThat(result).isNull();
    }

    @DisplayName("Redis 장애 시 포지션별 캐시 저장은 예외를 전파하지 않는다")
    @Test
    void saveChampionStatsByPosition_redisFailure_doesNotThrow() {
        // given
        List<PositionChampionStatsReadModel> stats = List.of(
            new PositionChampionStatsReadModel("TOP", List.of())
        );
        given(redisTemplate.opsForValue()).willThrow(new RuntimeException("Redis connection failed"));

        // when & then
        adapter.saveChampionStatsByPosition("16.1", "KR", "EMERALD", stats);
    }
}
