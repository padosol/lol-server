package com.example.lolserver.domain.league.service;

import java.util.List;

import com.example.lolserver.domain.league.domain.League;
import com.example.lolserver.repository.league.entity.LeagueSummonerEntity;
import com.example.lolserver.repository.league.entity.LeagueSummonerHistoryEntity;
import org.springframework.stereotype.Service;


import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LeagueService{

    private final LeagueSummonerFinder leagueSummonerFinder;

    public List<League> getLeaguesBypuuid(String puuid) {
        List<LeagueSummonerEntity> leagueSummoners = leagueSummonerFinder.findAllByPuuid(puuid);
        List<Long> leagueSummonerIds = leagueSummoners.stream().map(LeagueSummonerEntity::getId).toList();

        List<LeagueSummonerHistoryEntity> leagueSummonerHistories = leagueSummonerFinder
                .findAllHistoryByLeagueSummonerIds(leagueSummonerIds);

        return leagueSummoners.stream().map(leagueSummoner -> {
            League league = new League(leagueSummoner);
            Long leagueSummonerId = leagueSummoner.getId();

            List<LeagueSummonerHistoryEntity> histories = leagueSummonerHistories.stream()
                    .filter(history ->
                        history.getLeagueSummonerId().equals(leagueSummonerId)).toList();

            league.addAllHistory(histories);

            return league;
        }).toList();
    }
}