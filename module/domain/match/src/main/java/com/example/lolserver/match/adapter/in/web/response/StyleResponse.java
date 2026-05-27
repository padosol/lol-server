package com.example.lolserver.match.adapter.in.web.response;

import com.example.lolserver.match.application.model.readmodel.StyleReadModel;

/**
 * 매치 API 응답 - 참가자 룬 스타일.
 */
public record StyleResponse(
        int primaryStyleId,
        int primaryPerk0,
        int primaryPerk1,
        int primaryPerk2,
        int primaryPerk3,
        int subStyleId,
        int subPerk0,
        int subPerk1
) {
    public static StyleResponse from(StyleReadModel style) {
        return new StyleResponse(
                style.getPrimaryStyleId(),
                style.getPrimaryPerk0(),
                style.getPrimaryPerk1(),
                style.getPrimaryPerk2(),
                style.getPrimaryPerk3(),
                style.getSubStyleId(),
                style.getSubPerk0(),
                style.getSubPerk1()
        );
    }
}
