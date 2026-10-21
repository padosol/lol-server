package com.example.lolserver.duo.domain.vo;

import java.util.List;

/**
 * 듀오 게시글/요청 생성 시점에 스냅샷으로 저장하는 Riot 계정 정보 묶음.
 *
 * <p>전부 duo 도메인 VO(TierInfo/MostChampion/RecentGameSummary)와 문자열로 구성되므로
 * 도메인 팩토리가 직접 받을 수 있다. gameName·tagLine 은 매칭 상대에게 보여 줄 Riot ID 로 RSO 연동 값이다
 * ({@link RiotId}). 통계는 application 의 RiotAccountResolver 가 lol-repository 내부 API(duo-profile)로
 * 받는데, 받지 못하면 {@link #unknown} 으로 통계 없이 글·신청을 만든다 — lol-repository 상태가 듀오 사용을 막지 않는다.
 */
public record RiotAccountStats(
        String gameName,
        String tagLine,
        TierInfo tierInfo,
        List<MostChampion> mostChampions,
        RecentGameSummary recentGameSummary
) {

    public static RiotAccountStats of(RiotId riotId, SummonerStats stats) {
        return new RiotAccountStats(riotId.gameName(), riotId.tagLine(),
                stats.tierInfo(), stats.mostChampions(), stats.recentGameSummary());
    }

    /**
     * 통계를 받지 못했을 때(lol-repository 무응답·장애, 색인된 적 없는 소환사). 티어는 UNRANKED 표식으로
     * 저장되고(응답 {@code tierAvailable=false}), 언랭 검증은 확인할 수 없으므로 건너뛴다.
     */
    public static RiotAccountStats unknown(RiotId riotId) {
        return new RiotAccountStats(riotId.gameName(), riotId.tagLine(), null, List.of(), null);
    }

    public boolean isKnown() {
        return tierInfo != null;
    }

    /** 통계가 있으면 그 티어, 없으면 UNRANKED 표식 (duo_post·duo_request 의 tier 는 NOT NULL). */
    public TierInfo tierOrUnranked() {
        return isKnown() ? tierInfo : TierInfo.UNRANKED;
    }

    /** 솔로랭크 티어가 있어야 등록할 수 있다. 통계를 받지 못했으면 확인할 수 없으므로 막지 않는다. */
    public void validateEligible() {
        if (isKnown()) {
            tierInfo.validateRanked();
        }
    }
}
