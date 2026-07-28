package com.example.lolserver.summoner.adapter.in.web.response;

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
