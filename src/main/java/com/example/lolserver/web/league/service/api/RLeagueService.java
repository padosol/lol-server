package com.example.lolserver.web.league.service.api;

import com.example.lolserver.riot.type.Platform;
import com.example.lolserver.web.dto.data.LeagueData;
import com.example.lolserver.web.summoner.entity.Summoner;

import java.util.Set;

public interface RLeagueService {

    public LeagueData getLeagueSummoner(Summoner summoner);
}
