package com.example.lolserver.domain.league.application.port;

import com.example.lolserver.domain.league.domain.League;
import com.example.lolserver.domain.league.domain.vo.LeagueHistory;

import java.util.List;

public interface LeaguePersistencePort {
    List<League> findAllLeaguesByPuuid(String puuid);
    List<LeagueHistory> findAllHistoryByLeagueSummonerIds(List<Long> ids);

    // LP 시계열용: 한 리그(큐)의 최신 history 만 제한적으로 조회한다.
    List<LeagueHistory> findRecentHistoryByLeagueSummonerId(Long leagueSummonerId);
}
