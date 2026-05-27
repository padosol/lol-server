package com.example.lolserver.match.application.model.readmodel;

import java.util.List;

public record DailyGameCountSummaryReadModel(
    List<DailyGameCountReadModel> dailyCounts,
    long minCount,
    long maxCount
) {}
