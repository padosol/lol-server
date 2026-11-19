package com.example.lolserver.esports.domain.vo;

/** 계약 종료 사유. EXPIRED 는 저장하지 않고 조회 뷰가 유도한다 (자연 만료). */
public enum EndReason {
    EXPIRED,
    RENEWED,
    TRANSFERRED,
    RELEASED,
    RETIRED,
    RECALLED,
    MOVED
}
