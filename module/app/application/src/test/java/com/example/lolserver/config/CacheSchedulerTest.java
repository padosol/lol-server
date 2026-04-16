package com.example.lolserver.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CacheScheduler 테스트")
class CacheSchedulerTest {

    @DisplayName("evictRotationCache 메서드에 @Scheduled 어노테이션이 올바른 cron 표현식으로 설정된다")
    @Test
    void evictRotationCache_Scheduled어노테이션_cron표현식확인() throws NoSuchMethodException {
        // given
        Method method = CacheScheduler.class.getMethod("evictRotationCache");

        // when
        Scheduled scheduled = method.getAnnotation(Scheduled.class);

        // then
        assertThat(scheduled).isNotNull();
        assertThat(scheduled.cron()).isEqualTo("0 10 0 * * 2");
    }

    @DisplayName("evictRotationCache 메서드에 @CacheEvict 어노테이션이 rotation 캐시를 대상으로 설정된다")
    @Test
    void evictRotationCache_CacheEvict어노테이션_rotation캐시대상확인() throws NoSuchMethodException {
        // given
        Method method = CacheScheduler.class.getMethod("evictRotationCache");

        // when
        CacheEvict cacheEvict = method.getAnnotation(CacheEvict.class);

        // then
        assertThat(cacheEvict).isNotNull();
        assertThat(cacheEvict.value()).contains("rotation");
        assertThat(cacheEvict.allEntries()).isTrue();
    }
}
