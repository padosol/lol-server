package com.example.lolserver.esports.application.model.readmodel;

import java.time.LocalDate;

/** 시즌·대회 회차. */
public record SeasonReadModel(
        String seasonId,
        String leagueId,
        String name,
        String nameAcronym,
        Integer year,
        LocalDate startDate,
        LocalDate endDate,
        int sortOrder
) {
}
