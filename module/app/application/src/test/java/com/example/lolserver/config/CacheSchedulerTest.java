package com.example.lolserver.config;

import com.example.lolserver.gamedata.application.port.in.ChampionRotateUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;

import java.lang.reflect.Method;
import java.time.DayOfWeek;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class CacheSchedulerTest {

    @InjectMocks
    private CacheScheduler scheduler;

    @Mock
    private ChampionRotateUseCase championRotateUseCase;

    @Nested
    @DisplayName("evictRotationCache")
    class EvictRotationCache {

        @DisplayName("로테이션 캐시 제거 유스케이스를 호출한다")
        @Test
        void delegatesToUseCase() {
            // when
            scheduler.evictRotationCache();

            // then
            then(championRotateUseCase).should().evictChampionRotate();
        }

        @DisplayName("@Scheduled cron 은 매주 화요일 00:10 에 실행된다")
        @Test
        void scheduledEveryTuesdayAtTenPastMidnight() throws NoSuchMethodException {
            // given
            Method method = CacheScheduler.class.getDeclaredMethod("evictRotationCache");
            Scheduled scheduled = method.getAnnotation(Scheduled.class);
            assertThat(scheduled).isNotNull();

            // when: 화요일 00:10 실행 직후 기준 다음 발화 시각
            CronExpression cron = CronExpression.parse(scheduled.cron());
            LocalDateTime justAfterFiring = LocalDateTime.of(2026, 8, 11, 0, 11);
            LocalDateTime next = cron.next(justAfterFiring);

            // then: 정확히 일주일 뒤 화요일 00:10
            assertThat(next).isEqualTo(LocalDateTime.of(2026, 8, 18, 0, 10));
            assertThat(next.getDayOfWeek()).isEqualTo(DayOfWeek.TUESDAY);
        }
    }
}
