package com.example.lolserver.web.league.service;

import java.util.List;
import java.util.stream.Collectors;

import com.example.lolserver.web.exception.WebException;
import com.example.lolserver.web.summoner.entity.Summoner;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.example.lolserver.web.summoner.repository.SummonerJpaRepository;
import com.example.lolserver.web.dto.data.leagueData.LeagueSummonerData;
import com.example.lolserver.web.league.entity.LeagueSummoner;
import com.example.lolserver.web.league.repository.LeagueSummonerRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LeagueServiceImpl implements LeagueService{

    private final SummonerJpaRepository summonerJpaRepository;
    private final LeagueSummonerRepository leagueSummonerRepository;

    @Override
    public List<LeagueSummonerData> getLeaguesBypuuid(String puuid) {
        Summoner summoner = summonerJpaRepository.findById(puuid).orElseThrow(() -> new WebException(
                HttpStatus.BAD_REQUEST,
                "존재하지 않는 PUUID 입니다. " + puuid
        ));

        List<LeagueSummoner> leagueSummoners = leagueSummonerRepository.findAllBySummoner(summoner);

        return leagueSummoners.stream().map( LeagueSummoner::toData).collect(Collectors.toList());
    }
}