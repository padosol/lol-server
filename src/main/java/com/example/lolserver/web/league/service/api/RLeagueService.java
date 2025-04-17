package com.example.lolserver.web.league.service.api;

import java.util.List;
import java.util.Set;

import com.example.lolserver.web.summoner.entity.Summoner;
import com.example.lolserver.web.league.entity.LeagueSummoner;
import com.fasterxml.jackson.core.JsonProcessingException;

public interface RLeagueService {

    List<LeagueSummoner> getLeagueSummoner(Summoner summoner);

    Set<LeagueSummoner> getLeagueSummonerV2(Summoner summoner);

    void fetchSummonerLeague(Summoner summoner) throws JsonProcessingException;
}
