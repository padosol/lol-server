package com.example.lolserver.esports.domain.vo;

/** 계약 유지 중 비활동 사유. HIATUS 사건에만 붙는다. */
public enum HiatusReason {
    /** 휴식·부상 */
    REST,
    /** 출장정지 (징계) */
    SUSPENSION,
    /** 군 입대 */
    MILITARY,
    /** 개인 사유 (소속팀 동의 없는 임의 탈퇴 포함) */
    PERSONAL
}
