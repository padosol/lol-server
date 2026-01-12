package com.example.lolserver.domain.league.application;

import java.util.List;

import com.example.lolserver.domain.league.domain.League;
import com.example.lolserver.domain.league.domain.vo.LeagueHistory;
import com.example.lolserver.domain.league.application.port.LeaguePersistencePort;
import org.springframework.stereotype.Service;


import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LeagueService{

    private final LeaguePersistencePort leaguePersistencePort;

    public List<League> getLeaguesBypuuid(String puuid) {
        List<League> leagues = leaguePersistencePort.findAllLeaguesByPuuid(puuid);
        List<Long> leagueSummonerIds = leagues.stream().map(League::getId).toList();

        List<LeagueHistory> leagueHistories = leaguePersistencePort
                .findAllHistoryByLeagueSummonerIds(leagueSummonerIds);

        return leagues.stream().map(league -> {
            List<LeagueHistory> historiesForLeague = leagueHistories.stream()
                    .filter(history ->
                        history.leagueSummonerId().equals(league.getId())).toList();

            league.addAllHistoryDomain(historiesForLeague);

            return league;
        }).toList();
    }
}