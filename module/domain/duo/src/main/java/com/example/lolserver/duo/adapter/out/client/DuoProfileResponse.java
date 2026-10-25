package com.example.lolserver.duo.adapter.out.client;

import com.example.lolserver.duo.domain.vo.MostChampion;
import com.example.lolserver.duo.domain.vo.RecentGameSummary;
import com.example.lolserver.duo.domain.vo.SummonerStats;
import com.example.lolserver.duo.domain.vo.TierInfo;

import java.util.List;

/**
 * lol-repository {@code GET /internal/v1/summoners/{puuid}/duo-profile} 응답.
 * 계약은 lol-repository {@code docs/api/internal-v1.openapi.yaml} 이 소유한다.
 *
 * <p>응답의 gameName·tagLine 은 읽지 않는다 — Riot ID 는 RSO 연동 때 받은 값을 쓴다.
 */
public record DuoProfileResponse(
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

    public SummonerStats toDomain() {
        return new SummonerStats(
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
