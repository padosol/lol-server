package com.example.lolserver.web.league.repository;

import com.example.lolserver.web.league.entity.League;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LeagueRepository extends JpaRepository<League, String> {
}
