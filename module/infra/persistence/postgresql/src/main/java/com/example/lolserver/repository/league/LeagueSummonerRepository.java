package com.example.lolserver.repository.league;

import com.example.lolserver.repository.league.entity.LeagueSummonerEntity;
import com.example.lolserver.repository.league.entity.id.LeagueSummonerId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeagueSummonerRepository extends JpaRepository<LeagueSummonerEntity, LeagueSummonerId> {

    List<LeagueSummonerEntity> findAllByPuuid(String puuid);
}
