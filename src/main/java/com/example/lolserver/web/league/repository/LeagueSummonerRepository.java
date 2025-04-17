package com.example.lolserver.web.league.repository;

import java.util.List;

import com.example.lolserver.web.summoner.entity.Summoner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.lolserver.web.league.entity.LeagueSummoner;
import com.example.lolserver.web.league.entity.id.LeagueSummonerId;

@Repository
public interface LeagueSummonerRepository extends JpaRepository<LeagueSummoner, LeagueSummonerId> {

    List<LeagueSummoner> findAllBySummoner(Summoner summoner);
}
