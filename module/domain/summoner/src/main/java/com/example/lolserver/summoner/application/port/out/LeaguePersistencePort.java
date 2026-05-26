package com.example.lolserver.summoner.application.port.out;

import com.example.lolserver.summoner.domain.League;
import com.example.lolserver.summoner.domain.vo.LeagueHistory;

import java.util.List;

public interface LeaguePersistencePort {
    List<League> findAllLeaguesByPuuid(String puuid);
    List<LeagueHistory> findAllHistoryByLeagueSummonerIds(List<Long> ids);
}
