package com.example.lolserver.domain.league.service;

import com.example.lolserver.storage.db.core.repository.dto.data.leagueData.LeagueSummonerData;

import java.util.List;

public interface LeagueService {
    List<LeagueSummonerData> getLeaguesBypuuid(String puuid);
}
