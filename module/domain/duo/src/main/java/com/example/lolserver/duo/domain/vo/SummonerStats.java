package com.example.lolserver.duo.domain.vo;

import java.util.List;

/**
 * lol-repository 가 주는 소환사 통계 — 솔로 랭크 티어·모스트 챔피언·최근 게임 요약. Riot ID 는 담지 않는다
 * ({@link RiotId} 는 RSO 연동 값).
 */
public record SummonerStats(
        TierInfo tierInfo,
        List<MostChampion> mostChampions,
        RecentGameSummary recentGameSummary
) {
}
