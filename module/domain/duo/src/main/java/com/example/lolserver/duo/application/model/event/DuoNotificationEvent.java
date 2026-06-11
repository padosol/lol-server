package com.example.lolserver.duo.application.model.event;

/**
 * 듀오 실시간 알림 이벤트.
 *
 * <p>승인(ACCEPTED→요청자), 매칭 성사(→양측), 자동 탈락(CLOSED→탈락자) 시
 * {@code DuoNotificationPort}로 발행되어 대상 멤버에게 푸시된다.
 * 대상이 여럿(매칭 성사)이면 멤버별로 이벤트를 각각 발행한다.
 */
public record DuoNotificationEvent(
        DuoNotificationType type,
        Long targetMemberId,
        Long duoPostId,
        Long requestId
) {

    public enum DuoNotificationType {
        REQUEST_ACCEPTED,
        MATCH_CONFIRMED,
        REQUEST_CLOSED
    }
}
