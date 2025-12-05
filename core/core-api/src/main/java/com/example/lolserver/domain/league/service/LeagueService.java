package com.example.lolserver.domain.league.service;

import java.util.List;

import com.example.lolserver.controller.league.response.LeagueResponse;
import com.example.lolserver.storage.db.core.repository.league.entity.LeagueSummoner;
import com.example.lolserver.storage.db.core.repository.league.entity.LeagueSummonerHistory;
import org.springframework.stereotype.Service;


import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LeagueService{

    private final LeagueSummonerFinder leagueSummonerFinder;

    public LeagueResponse getLeaguesBypuuid(String puuid) {
        List<LeagueSummoner> leagueSummoners = leagueSummonerFinder.findAllByPuuid(puuid);
        List<Long> leagueSummonerIds = leagueSummoners.stream().map(LeagueSummoner::getId).toList();

        List<LeagueSummonerHistory> leagueSummonerHistories = leagueSummonerFinder
                .findAllHistoryByLeagueSummonerIds(leagueSummonerIds);

        return LeagueResponse.of(leagueSummoners, leagueSummonerHistories);
    }
}