package com.example.lolserver.esports.adapter.in.web.response;

import com.example.lolserver.esports.application.model.readmodel.SeasonReadModel;

import java.time.LocalDate;

public record SeasonResponse(
        String seasonId,
        String leagueId,
        String name,
        String nameAcronym,
        Integer year,
        LocalDate startDate,
        LocalDate endDate,
        int sortOrder
) {
    public static SeasonResponse from(SeasonReadModel model) {
        return new SeasonResponse(
                model.seasonId(),
                model.leagueId(),
                model.name(),
                model.nameAcronym(),
                model.year(),
                model.startDate(),
                model.endDate(),
                model.sortOrder());
    }
}
