package com.example.lolserver.esports.adapter.in.web.admin.response;

import com.example.lolserver.esports.domain.League;

public record LeagueResponse(
        String leagueId,
        String gameCode,
        String name,
        String nameAcronym,
        String region,
        boolean international,
        String imageUrl,
        String darkImageUrl,
        int sortOrder,
        boolean active
) {
    public static LeagueResponse from(League league) {
        return new LeagueResponse(league.getLeagueId(), league.getGameCode(), league.getName(),
                league.getNameAcronym(), league.getRegion(), league.isInternational(), league.getImageUrl(),
                league.getDarkImageUrl(), league.getSortOrder(), league.isActive());
    }
}
