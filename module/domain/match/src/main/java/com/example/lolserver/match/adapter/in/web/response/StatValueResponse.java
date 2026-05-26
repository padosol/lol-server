package com.example.lolserver.match.adapter.in.web.response;

import com.example.lolserver.match.application.model.StatValueReadModel;

/**
 * 매치 API 응답 - 참가자 룬 스탯.
 */
public record StatValueResponse(
        int defense,
        int flex,
        int offense
) {
    public static StatValueResponse from(StatValueReadModel value) {
        return new StatValueResponse(value.getDefense(), value.getFlex(), value.getOffense());
    }
}
