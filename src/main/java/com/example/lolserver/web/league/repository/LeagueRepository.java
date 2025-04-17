package com.example.lolserver.web.league.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.lolserver.web.league.entity.League;

@Repository
public interface LeagueRepository extends JpaRepository<League, String> {
}
