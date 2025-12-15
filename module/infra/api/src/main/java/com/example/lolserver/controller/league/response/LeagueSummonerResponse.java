package com.example.lolserver.controller.league.response;

public record LeagueSummonerResponse(
        String leagueType,
        int leaguePoints,
        int wins,
        int losses,
        String oow,
        String tier,
        String rank
) {
}
