package com.example.lolserver.esports.application.port.out;

import com.example.lolserver.esports.domain.League;
import com.example.lolserver.esports.domain.Season;

import java.util.Optional;

public interface LeaguePersistencePort {

    boolean existsLeague(String leagueId);

    Optional<League> findLeague(String leagueId);

    League saveLeague(League league);

    boolean existsSeason(String seasonId);

    Optional<Season> findSeason(String seasonId);

    Season saveSeason(Season season);
}
