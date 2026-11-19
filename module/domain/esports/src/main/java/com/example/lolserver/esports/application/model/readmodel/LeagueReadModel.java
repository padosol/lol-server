package com.example.lolserver.esports.application.model.readmodel;

/** 리그 (지역 리그 / 국제 대회). */
public record LeagueReadModel(
        String leagueId,
        String name,
        String nameAcronym,
        String region,
        boolean international,
        String imageUrl,
        String darkImageUrl,
        int sortOrder,
        boolean active
) {
}
