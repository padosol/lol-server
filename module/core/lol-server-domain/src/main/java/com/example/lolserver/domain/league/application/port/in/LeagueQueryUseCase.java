package com.example.lolserver.domain.league.application.port.in;

import com.example.lolserver.domain.league.domain.League;

import java.util.List;

public interface LeagueQueryUseCase {

    List<League> getLeaguesBypuuid(String puuid);

    // LP 시계열 그래프용: 큐별 최신 history 만 포함한 리그 리스트를 반환한다.
    List<League> getLpTimeline(String puuid);
}
