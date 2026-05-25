package com.example.lolserver.controller.league.response;

import java.time.LocalDateTime;

/**
 * LP 시계열 그래프의 한 데이터 포인트.
 * x축은 {@code timestamp}, y축은 {@code leaguePoints}(티어마다 0~100 으로 리셋) 또는
 * {@code absolutePoints}(티어를 가로질러 연속 증가하는 누적값) 중 하나를 사용한다.
 */
public record LpTimelinePoint(
        LocalDateTime timestamp,
        int leaguePoints,
        long absolutePoints,
        String tier,
        String rank
) {
}
