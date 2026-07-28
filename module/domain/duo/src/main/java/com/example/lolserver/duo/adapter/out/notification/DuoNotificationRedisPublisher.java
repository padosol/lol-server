package com.example.lolserver.duo.adapter.out.notification;

import com.example.lolserver.duo.adapter.notification.DuoNotificationChannels;
import com.example.lolserver.duo.application.model.event.DuoNotificationEvent;
import com.example.lolserver.duo.application.port.out.DuoNotificationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 듀오 알림을 Redis pub/sub 채널로 발행한다.
 *
 * <p>트랜잭션이 진행 중이면 커밋 이후(afterCommit)에 발행한다 — 롤백된 변경에 대한
 * 가짜 알림과 "알림이 커밋보다 먼저 도착"하는 순서 역전을 막는다.
 *
 * <p>값 직렬화는 common RedisConfig 의 {@code GenericJackson2JsonRedisSerializer}
 * (@class FQN 포함) — 이벤트 클래스 이동/리네임 시 구독 측 역직렬화가 깨진다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DuoNotificationRedisPublisher implements DuoNotificationPort {

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void notify(DuoNotificationEvent event) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            publish(event);
                        }
                    });
            return;
        }
        publish(event);
    }

    private void publish(DuoNotificationEvent event) {
        try {
            redisTemplate.convertAndSend(DuoNotificationChannels.DUO_NOTIFICATION, event);
        } catch (Exception e) {
            log.warn("듀오 알림 발행 실패 - type: {}, targetMemberId: {}, message: {}",
                    event.type(), event.targetMemberId(), e.getMessage());
        }
    }
}
