package com.example.lolserver.web.league.repository;

import com.example.lolserver.web.league.entity.LeagueSummoner;
import com.example.lolserver.web.league.entity.id.LeagueSummonerId;
import com.example.lolserver.web.summoner.entity.Summoner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeagueSummonerRepository extends JpaRepository<LeagueSummoner, LeagueSummonerId> {

    List<LeagueSummoner> findAllBySummoner(Summoner summoner);
}
