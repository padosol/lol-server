package com.example.lolserver.web.league.service;

import com.example.lolserver.web.dto.data.LeagueData;

import java.io.IOException;

public interface LeagueService {

    LeagueData getLeaguesBySummoner(String summonerId) throws IOException, InterruptedException;
}
