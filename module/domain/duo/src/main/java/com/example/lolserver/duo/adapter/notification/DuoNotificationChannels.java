package com.example.lolserver.duo.adapter.notification;

/**
 * 듀오 알림 Redis pub/sub 채널 정의.
 *
 * <p>발행(out 어댑터)과 구독(in 어댑터)이 함께 쓰는 인프라 상수라
 * in→out 직접 의존을 피하기 위해 adapter 중립 패키지에 둔다.
 */
public final class DuoNotificationChannels {

    public static final String DUO_NOTIFICATION = "duo:notification";

    private DuoNotificationChannels() {
    }
}
