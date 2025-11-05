package com.example.lolserver.storage.db.core.repository.league;

import com.example.lolserver.storage.db.core.repository.league.entity.League;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LeagueRepository extends JpaRepository<League, String> {
}
