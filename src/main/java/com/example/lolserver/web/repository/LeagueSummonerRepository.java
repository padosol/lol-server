package com.example.lolserver.web.repository;

import com.example.lolserver.entity.league.LeagueSummoner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LeagueSummonerRepository extends JpaRepository<LeagueSummoner, Long> {
}
