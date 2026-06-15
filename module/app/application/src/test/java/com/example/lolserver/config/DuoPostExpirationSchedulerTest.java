package com.example.lolserver.config;

import com.example.lolserver.duo.application.port.in.DuoPostExpirationUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Scheduled;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class DuoPostExpirationSchedulerTest {

    @InjectMocks
    private DuoPostExpirationScheduler scheduler;

    @Mock
    private DuoPostExpirationUseCase duoPostExpirationUseCase;

    @Nested
    @DisplayName("expireOverdueDuoPosts")
    class ExpireOverdueDuoPosts {

        @DisplayName("만료 유스케이스를 호출한다")
        @Test
        void delegatesToUseCase() {
            // when
            scheduler.expireOverdueDuoPosts();

            // then
            then(duoPostExpirationUseCase).should().expireOverduePosts();
        }

        @DisplayName("@Scheduled fixedDelay 1분 주기로 동작한다")
        @Test
        void scheduledWithFixedDelay() throws NoSuchMethodException {
            // given
            Method method = DuoPostExpirationScheduler.class
                    .getDeclaredMethod("expireOverdueDuoPosts");

            // when
            Scheduled scheduled = method.getAnnotation(Scheduled.class);

            // then
            assertThat(scheduled).isNotNull();
            assertThat(scheduled.fixedDelay()).isEqualTo(60_000L);
        }
    }
}
