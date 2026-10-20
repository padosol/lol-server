package com.example.lolserver.duo.domain.vo;

import java.util.List;

/**
 * 듀오 게시글/요청 생성 시점에 스냅샷으로 저장하는 Riot 계정 정보 묶음.
 *
 * <p>전부 duo 도메인 VO(TierInfo/MostChampion/RecentGameSummary)와 문자열로 구성되므로
 * 도메인 팩토리가 직접 받을 수 있다. 조회는 application 의 RiotAccountResolver 가
 * lol-repository 내부 API(duo-profile)로 한다. gameName·tagLine 은 매칭 상대에게 보여 줄 Riot ID 다.
 */
public record RiotAccountStats(
        String gameName,
        String tagLine,
        TierInfo tierInfo,
        List<MostChampion> mostChampions,
        RecentGameSummary recentGameSummary
) {
}
