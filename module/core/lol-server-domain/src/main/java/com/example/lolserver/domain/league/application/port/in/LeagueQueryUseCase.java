package com.example.lolserver.domain.league.application.port.in;

import com.example.lolserver.domain.league.domain.League;

import java.util.List;

public interface LeagueQueryUseCase {

    List<League> getLeaguesBypuuid(String puuid);
}
