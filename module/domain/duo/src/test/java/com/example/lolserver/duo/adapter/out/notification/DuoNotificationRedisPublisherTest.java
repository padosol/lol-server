package com.example.lolserver.duo.adapter.out.notification;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.example.lolserver.duo.application.model.event.DuoNotificationEvent;
import com.example.lolserver.duo.application.model.event.DuoNotificationEvent.DuoNotificationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

@ExtendWith(MockitoExtension.class)
class DuoNotificationRedisPublisherTest {

    @InjectMocks
    private DuoNotificationRedisPublisher publisher;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @DisplayName("notify 는 duo:notification 채널로 이벤트를 발행한다")
    @Test
    void notify_publishesToDuoNotificationChannel() {
        DuoNotificationEvent event = new DuoNotificationEvent(
                DuoNotificationType.REQUEST_ACCEPTED, 10L, 100L, 1000L);

        publisher.notify(event);

        then(redisTemplate).should().convertAndSend("duo:notification", event);
    }

    @DisplayName("notify 는 발행 실패 시 예외를 전파하지 않는다")
    @Test
    void notify_publishFailure_doesNotPropagate() {
        DuoNotificationEvent event = new DuoNotificationEvent(
                DuoNotificationType.MATCH_CONFIRMED, 10L, 100L, 1000L);
        given(redisTemplate.convertAndSend(any(String.class), any()))
                .willThrow(new RuntimeException("redis down"));

        assertThatCode(() -> publisher.notify(event)).doesNotThrowAnyException();
    }
}
