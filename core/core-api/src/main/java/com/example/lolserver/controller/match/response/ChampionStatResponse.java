package com.example.lolserver.controller.match.response;

public record ChampionStatResponse(
        long assists,
        long championId,
        long cs15,
        long csPerMin,
        long damageDealtPerMin,
        long damagePerTeam,
        long damageTakenPerMin,
        long deaths,
        long games,
        long goldDiff15,
        long goldPerTeam,
        long kda,
        long kills,
        long losses,
        long winRate,
        long wins
) {
}
