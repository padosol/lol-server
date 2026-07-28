package com.example.lolserver.match.adapter.in.web.response;

import com.example.lolserver.match.application.model.readmodel.DailyGameCountSummaryReadModel;

import java.util.List;

/**
 * daily-count API 응답 - 기간 내 날짜별 게임 수 + 최소/최대.
 */
public record DailyGameCountResponse(
        List<DailyGameCountItemResponse> dailyCounts,
        long minCount,
        long maxCount
) {
    public static DailyGameCountResponse from(DailyGameCountSummaryReadModel model) {
        return new DailyGameCountResponse(
                model.dailyCounts().stream().map(DailyGameCountItemResponse::from).toList(),
                model.minCount(),
                model.maxCount()
        );
    }
}
