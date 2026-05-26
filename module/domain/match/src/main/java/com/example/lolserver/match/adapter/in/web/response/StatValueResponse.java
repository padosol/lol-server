package com.example.lolserver.match.adapter.in.web.response;

import com.example.lolserver.match.domain.gamedata.value.StatValue;

/**
 * 매치 API 응답 - 참가자 룬 스탯.
 */
public record StatValueResponse(
        int defense,
        int flex,
        int offense
) {
    public static StatValueResponse from(StatValue value) {
        return new StatValueResponse(value.getDefense(), value.getFlex(), value.getOffense());
    }
}
