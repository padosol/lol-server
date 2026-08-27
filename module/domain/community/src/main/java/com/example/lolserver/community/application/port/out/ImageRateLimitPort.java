package com.example.lolserver.community.application.port.out;

public interface ImageRateLimitPort {

    /**
     * 회원의 분당 업로드 횟수를 1 증가시키고 한도 내인지 알려준다.
     *
     * <p>한도·정책 판단은 호출자가 한다 — 어댑터는 카운터일 뿐이다.
     *
     * @return 한도 내이면 true, 초과했으면 false
     */
    boolean tryAcquire(Long memberId, int limitPerMinute);
}
