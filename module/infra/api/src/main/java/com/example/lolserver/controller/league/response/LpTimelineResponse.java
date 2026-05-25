package com.example.lolserver.controller.league.response;

import java.util.List;

/**
 * 소환사의 솔로랭크/자유랭크 LP 변화 시계열 응답.
 * 각 리스트는 시간 오름차순(과거 → 현재)으로 정렬된 LP 데이터 포인트이며,
 * 프런트엔드 그래프 라이브러리에 그대로 전달할 수 있다.
 */
public record LpTimelineResponse(
        List<LpTimelinePoint> soloRankTimeline,
        List<LpTimelinePoint> flexRankTimeline
) {
}
