package com.example.lolserver.esports.adapter.in.web.response;

import com.example.lolserver.esports.application.model.readmodel.LeagueReadModel;

public record LeagueResponse(
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
    public static LeagueResponse from(LeagueReadModel model) {
        return new LeagueResponse(
                model.leagueId(),
                model.name(),
                model.nameAcronym(),
                model.region(),
                model.international(),
                model.imageUrl(),
                model.darkImageUrl(),
                model.sortOrder(),
                model.active());
    }
}
