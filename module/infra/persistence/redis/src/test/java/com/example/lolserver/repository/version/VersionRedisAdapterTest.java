package com.example.lolserver.repository.version;

import com.example.lolserver.domain.version.application.model.VersionReadModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class VersionRedisAdapterTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private VersionRedisAdapter adapter;

    private static final String CACHE_KEY = "version:latest";
    private static final Duration CACHE_TTL = Duration.ofHours(24);

    @BeforeEach
    void setUp() {
        adapter = new VersionRedisAdapter(redisTemplate);
    }

    @DisplayName("캐시에 버전 정보가 있으면 반환한다")
    @Test
    void findLatestVersion_cacheHit_returnsVersion() {
        // given
        VersionReadModel cachedVersion = new VersionReadModel(1L, "14.24.1", LocalDateTime.now());

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(CACHE_KEY)).willReturn(cachedVersion);

        // when
        VersionReadModel result = adapter.findLatestVersion();

        // then
        assertThat(result).isEqualTo(cachedVersion);
        assertThat(result.versionValue()).isEqualTo("14.24.1");
        then(redisTemplate).should().opsForValue();
        then(valueOperations).should().get(CACHE_KEY);
    }

    @DisplayName("캐시에 버전 정보가 없으면 null을 반환한다")
    @Test
    void findLatestVersion_cacheMiss_returnsNull() {
        // given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(CACHE_KEY)).willReturn(null);

        // when
        VersionReadModel result = adapter.findLatestVersion();

        // then
        assertThat(result).isNull();
        then(redisTemplate).should().opsForValue();
        then(valueOperations).should().get(CACHE_KEY);
    }

    @DisplayName("버전 정보를 저장하면 24시간 TTL로 캐시한다")
    @Test
    void saveLatestVersion_validData_savesWithTTL() {
        // given
        VersionReadModel version = new VersionReadModel(1L, "14.24.1", LocalDateTime.now());

        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        // when
        adapter.saveLatestVersion(version);

        // then
        then(valueOperations).should().set(eq(CACHE_KEY), eq(version), eq(CACHE_TTL));
    }

    @DisplayName("버전 정보가 null이면 저장하지 않는다")
    @Test
    void saveLatestVersion_nullData_doesNotSave() {
        // when
        adapter.saveLatestVersion(null);

        // then
        then(redisTemplate).shouldHaveNoInteractions();
    }
}
