package com.example.lolserver.esports.adapter.in.web.admin.response;

import com.example.lolserver.esports.domain.Season;

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
    public static SeasonResponse from(Season season) {
        return new SeasonResponse(season.getSeasonId(), season.getLeagueId(), season.getName(),
                season.getNameAcronym(), season.getYear(), season.getStartDate(), season.getEndDate(),
                season.getSortOrder());
    }
}
