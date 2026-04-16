package com.example.lolserver.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CacheConfig 테스트")
class CacheConfigTest {

    private CacheConfig cacheConfig;

    @BeforeEach
    void setUp() {
        cacheConfig = new CacheConfig();
    }

    @DisplayName("CacheManager 빈 생성 시 ConcurrentMapCacheManager를 반환한다")
    @Test
    void cacheManager_빈생성시_ConcurrentMapCacheManager반환() {
        // when
        CacheManager cacheManager = cacheConfig.cacheManager();

        // then
        assertThat(cacheManager).isInstanceOf(ConcurrentMapCacheManager.class);
    }

    @DisplayName("rotation 캐시가 정상적으로 등록된다")
    @Test
    void cacheManager_rotation캐시_정상등록() {
        // given
        CacheManager cacheManager = cacheConfig.cacheManager();

        // when
        Cache rotationCache = cacheManager.getCache("rotation");

        // then
        assertThat(rotationCache).isNotNull();
        assertThat(rotationCache.getName()).isEqualTo("rotation");
    }
}
