package com.example.lolserver.repository.match;

import com.example.lolserver.domain.match.application.model.GameReadModel;
import com.example.lolserver.support.SliceResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class MatchCacheAdapterTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private MatchCacheAdapter adapter;

    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    @BeforeEach
    void setUp() {
        adapter = new MatchCacheAdapter(redisTemplate);
    }

    @DisplayName("캐시 키는 season/queueId null 을 _ 토큰으로 채운다")
    @Test
    void findMatchesBatch_nullSeasonAndQueueId_usesUnderscoreToken() {
        // given
        String expectedKey = "match:list:v1:test-puuid:_:_:0";
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(expectedKey)).willReturn(null);

        // when
        SliceResult<GameReadModel> result = adapter.findMatchesBatch("test-puuid", null, null, 0);

        // then
        assertThat(result).isNull();
        then(valueOperations).should().get(expectedKey);
    }

    @DisplayName("캐시 히트 시 CachedSlice 를 SliceResult 로 복원해 반환한다")
    @Test
    void findMatchesBatch_cacheHit_returnsSliceResult() {
        // given
        String expectedKey = "match:list:v1:test-puuid:14:420:0";
        List<GameReadModel> games = List.of(new GameReadModel(), new GameReadModel());
        MatchCacheAdapter.CachedSlice cached = new MatchCacheAdapter.CachedSlice(games, true);

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(expectedKey)).willReturn(cached);

        // when
        SliceResult<GameReadModel> result = adapter.findMatchesBatch("test-puuid", 14, 420, 0);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEqualTo(games);
        assertThat(result.isHasNext()).isTrue();
    }

    @DisplayName("캐시 미스 시 null 을 반환한다")
    @Test
    void findMatchesBatch_cacheMiss_returnsNull() {
        // given
        String expectedKey = "match:list:v1:test-puuid:14:420:0";
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(expectedKey)).willReturn(null);

        // when
        SliceResult<GameReadModel> result = adapter.findMatchesBatch("test-puuid", 14, 420, 0);

        // then
        assertThat(result).isNull();
    }

    @DisplayName("Redis 장애 시 조회는 null 을 반환한다")
    @Test
    void findMatchesBatch_redisFailure_returnsNull() {
        // given
        given(redisTemplate.opsForValue()).willThrow(new RuntimeException("Redis connection failed"));

        // when
        SliceResult<GameReadModel> result = adapter.findMatchesBatch("test-puuid", 14, 420, 0);

        // then
        assertThat(result).isNull();
    }

    @DisplayName("SliceResult 를 CachedSlice 페이로드로 저장하며 TTL 30 분을 적용한다")
    @Test
    void saveMatchesBatch_validData_savesWithTtl() {
        // given
        String expectedKey = "match:list:v1:test-puuid:14:420:0";
        List<GameReadModel> games = List.of(new GameReadModel());
        SliceResult<GameReadModel> matches = new SliceResult<>(games, true);

        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        // when
        adapter.saveMatchesBatch("test-puuid", 14, 420, 0, matches);

        // then
        ArgumentCaptor<MatchCacheAdapter.CachedSlice> captor =
                ArgumentCaptor.forClass(MatchCacheAdapter.CachedSlice.class);
        then(valueOperations).should().set(eq(expectedKey), captor.capture(), eq(CACHE_TTL));
        MatchCacheAdapter.CachedSlice saved = captor.getValue();
        assertThat(saved.content()).isEqualTo(games);
        assertThat(saved.hasNext()).isTrue();
    }

    @DisplayName("null SliceResult 는 캐시에 저장하지 않는다")
    @Test
    void saveMatchesBatch_nullData_doesNotSave() {
        // when
        adapter.saveMatchesBatch("test-puuid", 14, 420, 0, null);

        // then
        then(redisTemplate).shouldHaveNoInteractions();
    }

    @DisplayName("Redis 장애 시 저장은 예외를 전파하지 않는다")
    @Test
    void saveMatchesBatch_redisFailure_doesNotThrow() {
        // given
        SliceResult<GameReadModel> matches = new SliceResult<>(List.of(new GameReadModel()), false);
        given(redisTemplate.opsForValue()).willThrow(new RuntimeException("Redis connection failed"));

        // when & then
        adapter.saveMatchesBatch("test-puuid", 14, 420, 0, matches);
    }

    @DisplayName("저장된 값 타입이 예상과 다르면 키를 삭제하고 null 을 반환한다")
    @Test
    void findMatchesBatch_unexpectedType_evictsAndReturnsNull() {
        // given
        String expectedKey = "match:list:v1:test-puuid:14:420:0";
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(expectedKey)).willReturn("stale-string");

        // when
        SliceResult<GameReadModel> result = adapter.findMatchesBatch("test-puuid", 14, 420, 0);

        // then
        assertThat(result).isNull();
        then(redisTemplate).should().delete(expectedKey);
    }

    @DisplayName("페이지 번호와 큐 ID 가 키에 정확히 반영된다")
    @Test
    void findMatchesBatch_keyIncludesAllParams() {
        // given
        String expectedKey = "match:list:v1:puuid-X:13:440:5";
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(expectedKey)).willReturn(null);

        // when
        adapter.findMatchesBatch("puuid-X", 13, 440, 5);

        // then
        then(valueOperations).should().get(expectedKey);
        then(valueOperations).should(never()).get("match:list:v1:puuid-X:13:440:0");
    }
}
