package com.example.lolserver.match.application.model.readmodel;

import java.time.LocalDate;

public record DailyGameCountReadModel(
    LocalDate gameDate,
    Long gameCount
) {}
