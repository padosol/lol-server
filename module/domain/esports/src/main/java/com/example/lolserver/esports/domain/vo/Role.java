package com.example.lolserver.esports.domain.vo;

/** 계약상 역할. 선수 계약과 코칭스태프 계약은 서로 다른 계약이다 (플레잉코치는 PLAYER). */
public enum Role {
    PLAYER,
    HEAD_COACH,
    COACH;

    public boolean isStaff() {
        return this != PLAYER;
    }
}
