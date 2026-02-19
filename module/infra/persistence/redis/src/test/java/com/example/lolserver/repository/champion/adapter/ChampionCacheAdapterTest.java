package com.example.lolserver.repository.champion.adapter;

import com.example.lolserver.domain.champion.domain.ChampionRotate;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class ChampionCacheAdapterTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private ChampionCacheAdapter adapter;

    private static final String CACHE_KEY_PREFIX = "champion_rotation_";

    @BeforeEach
    void setUp() {
        adapter = new ChampionCacheAdapter(redisTemplate);
    }

    @DisplayName("캐시에 챔피언 로테이션이 존재하면 Optional로 반환한다")
    @Test
    void getChampionRotate_cacheHit_returnsChampionRotate() {
        // given
        String region = "kr";
        ChampionRotate cachedRotate = new ChampionRotate(
                10,
                List.of(18, 81, 22),
                List.of(1, 2, 3, 4, 5)
        );

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(CACHE_KEY_PREFIX + region)).willReturn(cachedRotate);

        // when
        Optional<ChampionRotate> result = adapter.getChampionRotate(region);

        // then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(cachedRotate);
        assertThat(result.get().getMaxNewPlayerLevel()).isEqualTo(10);
        assertThat(result.get().getFreeChampionIds()).hasSize(5);
        then(redisTemplate).should().opsForValue();
        then(valueOperations).should().get(CACHE_KEY_PREFIX + region);
    }

    @DisplayName("캐시에 챔피언 로테이션이 없으면 빈 Optional을 반환한다")
    @Test
    void getChampionRotate_cacheMiss_returnsEmpty() {
        // given
        String region = "kr";

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(CACHE_KEY_PREFIX + region)).willReturn(null);

        // when
        Optional<ChampionRotate> result = adapter.getChampionRotate(region);

        // then
        assertThat(result).isEmpty();
        then(redisTemplate).should().opsForValue();
        then(valueOperations).should().get(CACHE_KEY_PREFIX + region);
    }

    @DisplayName("챔피언 로테이션을 캐시에 저장한다")
    @Test
    void saveChampionRotate_validData_savesToCache() {
        // given
        String region = "kr";
        ChampionRotate championRotate = new ChampionRotate(
                10,
                List.of(18, 81, 22, 21, 36),
                List.of(6, 7, 8, 9, 10, 11, 12)
        );

        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        // when
        adapter.saveChampionRotate(region, championRotate);

        // then
        then(redisTemplate).should().opsForValue();
        then(valueOperations).should().set(
                eq(CACHE_KEY_PREFIX + region),
                eq(championRotate),
                eq(Duration.ofHours(1))
        );
    }

    @DisplayName("다른 지역의 챔피언 로테이션을 각각 캐시에 저장하고 조회할 수 있다")
    @Test
    void getChampionRotate_differentRegions_returnsCorrectData() {
        // given
        String regionKr = "kr";
        String regionNa = "na1";

        ChampionRotate krRotate = new ChampionRotate(10, List.of(1, 2, 3), List.of(100, 101));
        ChampionRotate naRotate = new ChampionRotate(10, List.of(4, 5, 6), List.of(200, 201));

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(CACHE_KEY_PREFIX + regionKr)).willReturn(krRotate);
        given(valueOperations.get(CACHE_KEY_PREFIX + regionNa)).willReturn(naRotate);

        // when
        Optional<ChampionRotate> krResult = adapter.getChampionRotate(regionKr);
        Optional<ChampionRotate> naResult = adapter.getChampionRotate(regionNa);

        // then
        assertThat(krResult).isPresent();
        assertThat(krResult.get().getFreeChampionIds()).containsExactly(100, 101);

        assertThat(naResult).isPresent();
        assertThat(naResult.get().getFreeChampionIds()).containsExactly(200, 201);
    }
}
