package com.example.lolserver.domain.duo.domain.vo;

public record TierInfo(
        String tier,
        String rank,
        int leaguePoints
) {
    public static final TierInfo UNRANKED = new TierInfo(null, null, 0);
}
