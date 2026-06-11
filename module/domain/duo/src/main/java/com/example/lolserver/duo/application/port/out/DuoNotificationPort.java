package com.example.lolserver.duo.application.port.out;

import com.example.lolserver.duo.application.model.event.DuoNotificationEvent;

/**
 * 듀오 실시간 알림 발행 포트.
 *
 * <p>다중 인스턴스(k8s)에서 SSE 연결 인스턴스와 이벤트 발생 인스턴스가 다를 수 있으므로
 * 구현체는 Redis pub/sub 으로 전 인스턴스에 fanout 한다.
 */
public interface DuoNotificationPort {

    void notify(DuoNotificationEvent event);
}
