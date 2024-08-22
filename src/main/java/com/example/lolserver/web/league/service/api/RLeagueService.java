package com.example.lolserver.web.league.service.api;

import com.example.lolserver.riot.type.Platform;
import com.example.lolserver.web.dto.data.LeagueData;
import com.example.lolserver.web.league.entity.LeagueSummoner;
import com.example.lolserver.web.summoner.entity.Summoner;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.List;
import java.util.Set;

public interface RLeagueService {

    List<LeagueSummoner> getLeagueSummoner(Summoner summoner);

    Set<LeagueSummoner> getLeagueSummonerV2(Summoner summoner);

    void fetchSummonerLeague(Summoner summoner) throws JsonProcessingException;
}
