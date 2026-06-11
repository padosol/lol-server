package com.example.lolserver.duo.adapter.in.sse;

import com.example.lolserver.common.web.security.AuthenticatedMember;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 듀오 알림 SSE 구독 엔드포인트(인증 필수).
 *
 * <p>구독 직후 connect 더미 이벤트를 보내 연결 수립을 클라이언트에 알린다
 * (503 방지 + 프록시 버퍼 flush).
 */
@RestController
@RequestMapping("/api/duo")
@RequiredArgsConstructor
public class DuoNotificationController {

    private static final long TIMEOUT_MILLIS = 30L * 60 * 1000;

    private final SseEmitterRegistry sseEmitterRegistry;

    @GetMapping(value = "/notifications/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@AuthenticationPrincipal AuthenticatedMember member) {
        SseEmitter emitter = sseEmitterRegistry.register(
                member.memberId(), new SseEmitter(TIMEOUT_MILLIS));
        sseEmitterRegistry.send(member.memberId(), "connect", "connected");
        return emitter;
    }
}
