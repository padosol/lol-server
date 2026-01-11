package com.example.lolserver.domain.league.application.port;

import com.example.lolserver.domain.league.domain.League;
import com.example.lolserver.domain.league.domain.vo.LeagueHistory;

import java.util.List;

public interface LeaguePersistencePort {
    List<League> findAllLeaguesByPuuid(String puuid);
    List<LeagueHistory> findAllHistoryByLeagueSummonerIds(List<Long> ids);
}
