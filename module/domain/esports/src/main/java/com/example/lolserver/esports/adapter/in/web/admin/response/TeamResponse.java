package com.example.lolserver.esports.adapter.in.web.admin.response;

import com.example.lolserver.esports.domain.Team;

import java.time.LocalDate;

public record TeamResponse(
        String teamId,
        String teamCode,
        String gameCode,
        String homeLeagueId,
        String parentTeamId,
        String name,
        String nameEng,
        String imageUrl,
        String darkImageUrl,
        LocalDate foundedAt,
        LocalDate disbandedAt,
        LocalDate nameSince
) {
    public static TeamResponse from(Team team) {
        return new TeamResponse(team.getTeamId(), team.getTeamCode(), team.getGameCode(), team.getHomeLeagueId(),
                team.getParentTeamId(), team.getName(), team.getNameEng(), team.getImageUrl(),
                team.getDarkImageUrl(), team.getFoundedAt(), team.getDisbandedAt(), team.getNameSince());
    }
}
