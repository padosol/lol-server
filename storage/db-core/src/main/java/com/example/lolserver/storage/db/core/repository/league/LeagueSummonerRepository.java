package com.example.lolserver.storage.db.core.repository.league;

import com.example.lolserver.storage.db.core.repository.league.entity.LeagueSummoner;
import com.example.lolserver.storage.db.core.repository.league.entity.id.LeagueSummonerId;
import com.example.lolserver.storage.db.core.repository.summoner.entity.Summoner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeagueSummonerRepository extends JpaRepository<LeagueSummoner, LeagueSummonerId> {

    List<LeagueSummoner> findAllByPuuid(String puuid);
}
