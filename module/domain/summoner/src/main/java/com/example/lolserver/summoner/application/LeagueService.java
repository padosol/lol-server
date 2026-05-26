package com.example.lolserver.summoner.application;

import java.util.List;

import com.example.lolserver.summoner.application.model.LeagueReadModel;
import com.example.lolserver.summoner.domain.League;
import com.example.lolserver.summoner.domain.vo.LeagueHistory;
import com.example.lolserver.summoner.application.port.out.LeaguePersistencePort;
import com.example.lolserver.summoner.application.port.in.LeagueQueryUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LeagueService implements LeagueQueryUseCase {

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

    public List<LeagueReadModel> getLeagueSummariesByPuuid(String puuid) {
        return leaguePersistencePort.findAllLeaguesByPuuid(puuid).stream()
                .map(LeagueReadModel::of)
                .toList();
    }
}
