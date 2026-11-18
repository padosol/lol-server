package com.example.lolserver.community.domain.vo;

import java.util.Objects;
import java.util.UUID;

/** 조회수 중복 판정에서 "누가 봤는가"를 가리키는 키. 회원은 m:, 비로그인 방문자는 v: 접두사. */
public record ViewerKey(String value) {

    public static ViewerKey ofMember(Long memberId) {
        Objects.requireNonNull(memberId, "memberId");
        return new ViewerKey("m:" + memberId);
    }

    public static ViewerKey ofVisitor(UUID visitorId) {
        Objects.requireNonNull(visitorId, "visitorId");
        return new ViewerKey("v:" + visitorId);
    }
}
