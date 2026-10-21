package com.example.lolserver.duo.application.port.out;

import com.example.lolserver.duo.domain.vo.SummonerStats;

import java.util.Optional;

/**
 * 소환사 통계 조회. lol-server 는 소환사·리그·매치 데이터를 갖지 않으므로 그 주인인 lol-repository 에 묻는다.
 * Riot ID 는 묻지 않는다 — 듀오는 RSO 연동 회원만 쓰므로 연동 때 받은 값을 쓴다.
 *
 * <p>듀오는 이 조회 없이도 동작해야 하므로, 받지 못한 이유(무응답·장애·색인 없음)와 무관하게 빈 값을 돌려주고
 * 예외를 던지지 않는다.
 */
public interface SummonerStatsPort {

    /**
     * 솔로 랭크 티어·모스트 챔피언·최근 게임 요약.
     *
     * @return lol-repository 가 응답하지 않거나 색인된 적 없는 계정이면 빈 값
     */
    Optional<SummonerStats> findByPuuid(String puuid);
}
