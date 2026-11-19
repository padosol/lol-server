package com.example.lolserver.esports.application.command;

/** 리그 등록·수정. 수정할 때 leagueId 는 경로 값을 쓴다. */
public record LeagueCommand(
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
