package com.example.lolserver.web.repository;

import com.example.lolserver.entity.league.LeagueSummoner;
import com.example.lolserver.web.summoner.entity.Summoner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeagueSummonerRepository extends JpaRepository<LeagueSummoner, Long> {

    List<LeagueSummoner> findAllBySummoner(Summoner summoner);
}
