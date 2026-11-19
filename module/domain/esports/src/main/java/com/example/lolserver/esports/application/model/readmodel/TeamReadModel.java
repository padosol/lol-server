package com.example.lolserver.esports.application.model.readmodel;

import java.time.LocalDate;

/** 팀. CL 팀은 parentTeamId 로 같은 조직의 LCK 팀을 가리킨다. */
public record TeamReadModel(
        String teamId,
        String teamCode,
        String name,
        String nameEng,
        String imageUrl,
        String darkImageUrl,
        String homeLeagueId,
        String parentTeamId,
        LocalDate foundedAt,
        LocalDate disbandedAt,
        LocalDate nameSince
) {
}
