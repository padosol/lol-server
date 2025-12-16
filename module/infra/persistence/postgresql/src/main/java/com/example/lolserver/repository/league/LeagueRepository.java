package com.example.lolserver.repository.league;

import com.example.lolserver.repository.league.entity.LeagueEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LeagueRepository extends JpaRepository<LeagueEntity, String> {
}
