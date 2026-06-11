package com.example.lolserver.duo.adapter.in.sse;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.example.lolserver.duo.application.model.event.DuoNotificationEvent;
import com.example.lolserver.duo.application.model.event.DuoNotificationEvent.DuoNotificationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;

@ExtendWith(MockitoExtension.class)
class DuoNotificationRedisSubscriberTest {

    private static final byte[] CHANNEL = "duo:notification".getBytes();

    @InjectMocks
    private DuoNotificationRedisSubscriber subscriber;

    @Mock
    private SseEmitterRegistry sseEmitterRegistry;

    private final GenericJackson2JsonRedisSerializer serializer =
            new GenericJackson2JsonRedisSerializer();

    @DisplayName("이벤트 메시지를 수신하면 대상 멤버에게 SSE 로 전달한다")
    @Test
    void onMessage_event_sendsToTargetMember() {
        DuoNotificationEvent event = new DuoNotificationEvent(
                DuoNotificationType.REQUEST_CLOSED, 10L, 100L, 1000L);
        Message message = new DefaultMessage(CHANNEL, serializer.serialize(event));

        subscriber.onMessage(message, null);

        then(sseEmitterRegistry).should()
                .send(eq(10L), eq(DuoNotificationRedisSubscriber.SSE_EVENT_NAME), eq(event));
    }

    @DisplayName("역직렬화 불가능한 메시지는 무시하고 예외를 전파하지 않는다")
    @Test
    void onMessage_invalidPayload_ignored() {
        Message message = new DefaultMessage(CHANNEL, "not-json".getBytes());

        assertThatCode(() -> subscriber.onMessage(message, null))
                .doesNotThrowAnyException();
        then(sseEmitterRegistry).should(never())
                .send(anyLong(), any(String.class), any());
    }

    @DisplayName("이벤트 타입이 아닌 페이로드는 전송하지 않는다")
    @Test
    void onMessage_unexpectedType_notSent() {
        Message message = new DefaultMessage(CHANNEL, serializer.serialize("plain-string"));

        subscriber.onMessage(message, null);

        then(sseEmitterRegistry).should(never())
                .send(anyLong(), any(String.class), any());
    }
}
