package com.example.lolserver.duo.domain.vo;

import java.util.List;

/**
 * 듀오 게시글/요청 생성에 쓰이는, 상위 컨텍스트에서 해석한 플레이어 전적 요약 묶음.
 *
 * <p>전부 duo 도메인 VO(TierInfo/MostChampion/RecentGameSummary)로 구성되므로
 * 도메인 팩토리가 직접 받을 수 있다. 상위 컨텍스트 조회는 application 의 RiotAccountResolver 가 수행한다.
 */
public record RiotAccountStats(
        TierInfo tierInfo,
        List<MostChampion> mostChampions,
        RecentGameSummary recentGameSummary
) {
}
