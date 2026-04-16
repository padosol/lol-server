package com.example.lolserver.domain.match.application.model;

import java.util.List;

public record DailyGameCountSummaryReadModel(
    List<DailyGameCountReadModel> dailyCounts,
    long minCount,
    long maxCount
) {}
