package com.example.lolserver.domain.match.application.dto;

import java.time.LocalDate;

public record DailyGameCountResponse(
    LocalDate gameDate,
    Long gameCount
) {}
