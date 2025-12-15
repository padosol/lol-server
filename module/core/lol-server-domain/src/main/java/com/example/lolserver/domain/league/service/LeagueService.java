package com.example.lolserver.domain.league.service;

import java.util.List;

import com.example.lolserver.common.response.LeagueResponse;
import com.example.lolserver.domain.league.domain.League;
import com.example.lolserver.storage.db.core.repository.league.entity.LeagueSummoner;
import com.example.lolserver.storage.db.core.repository.league.entity.LeagueSummonerHistory;
import org.springframework.stereotype.Service;


import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LeagueService{

    private final LeagueSummonerFinder leagueSummonerFinder;

    public List<League> getLeaguesBypuuid(String puuid) {
        List<LeagueSummoner> leagueSummoners = leagueSummonerFinder.findAllByPuuid(puuid);
        List<Long> leagueSummonerIds = leagueSummoners.stream().map(LeagueSummoner::getId).toList();

        List<LeagueSummonerHistory> leagueSummonerHistories = leagueSummonerFinder
                .findAllHistoryByLeagueSummonerIds(leagueSummonerIds);

        return leagueSummoners.stream().map(leagueSummoner -> {
            League league = new League(leagueSummoner);
            Long leagueSummonerId = leagueSummoner.getId();

            List<LeagueSummonerHistory> histories = leagueSummonerHistories.stream().filter(history ->
                    history.getLeagueSummonerId().equals(leagueSummonerId)).toList();

            league.addAllHistory(histories);

            return league;
        }).toList();
    }
}