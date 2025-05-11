package com.example.lolserver.web.league.service;

import com.example.lolserver.web.dto.data.leagueData.LeagueSummonerData;

import java.util.List;

public interface LeagueService {
    List<LeagueSummonerData> getLeaguesBypuuid(String puuid);
}
