package com.example.lolserver.controller.league.response;

import java.util.List;

public record LeagueResponse(
    LeagueSummonerResponse soloLeague,
    LeagueSummonerResponse flexLeague,

    List<LeagueSummonerResponse> soloLeagueHistory,
    List<LeagueSummonerResponse> flexLeagueHistory
) {
}
