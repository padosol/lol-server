package com.example.lolserver.domain.league.service;

import com.example.lolserver.controller.league.response.LeagueResponse;
import com.example.lolserver.storage.db.core.repository.dto.data.leagueData.LeagueSummonerData;
import com.example.lolserver.storage.db.core.repository.league.entity.LeagueSummonerDetail;

import java.util.List;

public interface LeagueService {
    LeagueResponse getLeaguesBypuuid(String puuid);
}
