package com.example.lolserver.duo.application.port.out;

import com.example.lolserver.duo.domain.vo.RiotAccountStats;

import java.util.Optional;

/**
 * Riot 계정 정보 조회. lol-server 는 소환사·리그·매치 데이터를 갖지 않으므로 그 주인인 lol-repository 에 묻는다.
 *
 * <p>lol-repository 가 응답하지 않으면 {@code RIOT_ACCOUNT_STATS_UNAVAILABLE} 을 던진다.
 */
public interface RiotAccountStatsPort {

    /**
     * Riot ID 와 솔로 랭크 티어·모스트 챔피언·최근 게임 요약.
     *
     * @return 소환사 검색으로 색인된 적 없는 계정이면 빈 값 — 실제 언랭(UNRANKED 티어)과 구분한다
     */
    Optional<RiotAccountStats> findByPuuid(String puuid);
}
