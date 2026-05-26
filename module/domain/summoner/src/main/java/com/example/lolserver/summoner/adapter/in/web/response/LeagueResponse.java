package com.example.lolserver.summoner.adapter.in.web.response;

import java.util.List;

public record LeagueResponse(
    LeagueSummonerResponse soloLeague,
    LeagueSummonerResponse flexLeague,

    List<LeagueSummonerResponse> soloLeagueHistory,
    List<LeagueSummonerResponse> flexLeagueHistory
) {
}
