package com.example.lolserver.domain.match.application.model;

import java.time.LocalDate;

public record DailyGameCountReadModel(
    LocalDate gameDate,
    Long gameCount
) {}
