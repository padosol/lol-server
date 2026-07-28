package com.example.lolserver.duo.adapter.in.sse;

import com.example.lolserver.duo.application.model.event.DuoNotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.stereotype.Component;

/**
 * Redis pub/sub 채널에서 듀오 알림 이벤트를 수신해, 이 파드에 SSE 연결된 대상 멤버에게 전달한다.
 *
 * <p>역직렬화는 발행 측(common RedisConfig 의 RedisTemplate 값 직렬화)과 동일한
 * {@code GenericJackson2JsonRedisSerializer}(@class FQN 포함) 를 사용한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DuoNotificationRedisSubscriber implements MessageListener {

    public static final String SSE_EVENT_NAME = "duo-notification";

    private final SseEmitterRegistry sseEmitterRegistry;
    private final GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer();

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            Object deserialized = serializer.deserialize(message.getBody());
            if (deserialized instanceof DuoNotificationEvent event) {
                sseEmitterRegistry.send(event.targetMemberId(), SSE_EVENT_NAME, event);
            } else {
                log.warn("듀오 알림 메시지 타입 불일치 - type: {}",
                        deserialized == null ? null : deserialized.getClass().getName());
            }
        } catch (Exception e) {
            log.warn("듀오 알림 메시지 처리 실패 - message: {}", e.getMessage());
        }
    }
}
