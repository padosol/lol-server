package com.example.lolserver.duo.adapter.in.sse;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 멤버별 SSE 연결(emitter) 보관소. 이 파드에 연결된 멤버에게만 전송한다.
 *
 * <p>완료/타임아웃/에러 콜백과 전송 실패(IOException) 시 제거되어 죽은 연결이 남지 않는다.
 * 멤버당 1연결 정책 — 재구독 시 기존 emitter 를 덮어쓴다.
 */
@Slf4j
@Component
public class SseEmitterRegistry {

    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter register(Long memberId, SseEmitter emitter) {
        emitters.put(memberId, emitter);
        emitter.onCompletion(() -> emitters.remove(memberId, emitter));
        emitter.onTimeout(() -> emitters.remove(memberId, emitter));
        emitter.onError(e -> emitters.remove(memberId, emitter));
        return emitter;
    }

    public void send(Long memberId, String eventName, Object data) {
        SseEmitter emitter = emitters.get(memberId);
        if (emitter == null) {
            return;
        }
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (IOException e) {
            log.debug("SSE 전송 실패로 emitter 제거 - memberId: {}, message: {}", memberId, e.getMessage());
            emitters.remove(memberId, emitter);
            emitter.completeWithError(e);
        }
    }

    public boolean contains(Long memberId) {
        return emitters.containsKey(memberId);
    }
}
