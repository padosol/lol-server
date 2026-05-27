package com.example.lolserver.match.adapter.in.web.response;

import com.example.lolserver.match.application.model.readmodel.DailyGameCountReadModel;

import java.time.LocalDate;

/**
 * daily-count API 응답 - 날짜별 게임 수 단건.
 */
public record DailyGameCountItemResponse(
        LocalDate gameDate,
        Long gameCount
) {
    public static DailyGameCountItemResponse from(DailyGameCountReadModel model) {
        return new DailyGameCountItemResponse(model.gameDate(), model.gameCount());
    }
}
