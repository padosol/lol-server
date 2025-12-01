package com.example.lolserver.domain.league.service;

import java.util.List;
import java.util.stream.Collectors;

import com.example.lolserver.controller.league.response.LeagueResponse;
import com.example.lolserver.storage.db.core.repository.league.LeagueSummonerDetailRepository;
import com.example.lolserver.storage.db.core.repository.league.entity.LeagueSummonerDetail;
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

    private final LeagueSummonerDetailRepository leagueSummonerDetailRepository;

    @Override
    public LeagueResponse getLeaguesBypuuid(String puuid) {
        List<LeagueSummonerDetail> leagueSummonerDetails = leagueSummonerDetailRepository.findAllByPuuid(puuid);

        return LeagueResponse.of(leagueSummonerDetails);
    }
}