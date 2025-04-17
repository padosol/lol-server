package com.example.lolserver.web.league.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.example.lolserver.web.summoner.entity.Summoner;
import org.springframework.stereotype.Service;

import com.example.lolserver.web.summoner.repository.SummonerJpaRepository;
import com.example.lolserver.web.dto.data.LeagueData;
import com.example.lolserver.web.dto.data.leagueData.LeagueSummonerData;
import com.example.lolserver.web.league.entity.LeagueSummoner;
import com.example.lolserver.web.league.repository.LeagueSummonerRepository;
import com.example.lolserver.web.league.service.api.RLeagueService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LeagueServiceImpl implements LeagueService{

    private final SummonerJpaRepository summonerJpaRepository;
    private final LeagueSummonerRepository leagueSummonerRepository;
    private final RLeagueService rLeagueService;

    @Override
    public LeagueData getLeaguesBySummoner(String summonerId) {

        Optional<Summoner> summonerOptional = summonerJpaRepository.findById(summonerId);
        if(summonerOptional.isEmpty()) {
            return new LeagueData(true);
        }

        LeagueData leagueData = new LeagueData();
        Summoner summoner = summonerOptional.get();

        List<LeagueSummoner> leagueSummoners = leagueSummonerRepository.findAllBySummoner(summoner);

        if(leagueSummoners.size() == 0) {

            List<LeagueSummonerData> result = rLeagueService.getLeagueSummoner(summoner).stream().map(LeagueSummoner::toData).toList();
            leagueData.setLeagues(result);

            return leagueData;
        }

        leagueData.setLeagues(leagueSummoners.stream().map( LeagueSummoner::toData).collect(Collectors.toList()));
        return leagueData;
    }

}
