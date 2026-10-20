package com.example.lolserver.duo.adapter.out.client;

import com.example.lolserver.duo.domain.vo.MostChampion;
import com.example.lolserver.duo.domain.vo.RecentGameSummary;
import com.example.lolserver.duo.domain.vo.RiotAccountStats;
import com.example.lolserver.duo.domain.vo.TierInfo;

import java.util.List;

/**
 * lol-repository {@code GET /internal/v1/summoners/{puuid}/duo-profile} 응답.
 * 계약은 lol-repository {@code docs/api/internal-v1.openapi.yaml} 이 소유한다.
 */
public record DuoProfileResponse(
        String gameName,
        String tagLine,
        Tier tierInfo,
        List<Champion> mostChampions,
        RecentGames recentGameSummary
) {

    public record Tier(String tier, String rank, int leaguePoints) {
    }

    public record Champion(int championId, String championName, long playCount, long wins, long losses) {
    }

    public record RecentGames(int wins, int losses, List<PlayedChampion> playedChampions) {
    }

    public record PlayedChampion(int championId, String championName) {
    }

    public RiotAccountStats toDomain() {
        return new RiotAccountStats(
                gameName,
                tagLine,
                new TierInfo(tierInfo.tier(), tierInfo.rank(), tierInfo.leaguePoints()),
                mostChampions.stream()
                        .map(c -> new MostChampion(
                                c.championId(), c.championName(), c.playCount(), c.wins(), c.losses()))
                        .toList(),
                new RecentGameSummary(
                        recentGameSummary.wins(),
                        recentGameSummary.losses(),
                        recentGameSummary.playedChampions().stream()
                                .map(p -> new RecentGameSummary.PlayedChampion(p.championId(), p.championName()))
                                .toList()));
    }
}
