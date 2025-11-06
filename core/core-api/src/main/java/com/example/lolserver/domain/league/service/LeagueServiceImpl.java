package com.example.lolserver.domain.league.service;

import java.util.List;
import java.util.stream.Collectors;

import com.example.lolserver.support.error.ErrorType;
import com.example.lolserver.storage.db.core.repository.dto.data.leagueData.LeagueSummonerData;
import com.example.lolserver.storage.db.core.repository.league.LeagueSummonerRepository;
import com.example.lolserver.storage.db.core.repository.league.entity.LeagueSummoner;
import com.example.lolserver.storage.db.core.repository.summoner.SummonerJpaRepository;
import com.example.lolserver.storage.db.core.repository.summoner.entity.Summoner;
import com.example.lolserver.support.error.CoreException;
import org.springframework.stereotype.Service;


import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LeagueServiceImpl implements LeagueService{

    private final SummonerJpaRepository summonerJpaRepository;
    private final LeagueSummonerRepository leagueSummonerRepository;

    @Override
    public List<LeagueSummonerData> getLeaguesBypuuid(String puuid) {
        Summoner summoner = summonerJpaRepository.findById(puuid).orElseThrow(() -> new CoreException(
                ErrorType.NOT_FOUND_PUUID,
                "존재하지 않는 PUUID 입니다. " + puuid
        ));

        List<LeagueSummoner> leagueSummoners = leagueSummonerRepository.findAllBySummoner(summoner);

        return leagueSummoners.stream().map( LeagueSummoner::toData).collect(Collectors.toList());
    }
}