package com.example.lolserver.summoner.adapter.out.persistence.repository;

import com.example.lolserver.summoner.adapter.out.persistence.entity.LeagueSummonerEntity;
import com.example.lolserver.summoner.adapter.out.persistence.entity.id.LeagueSummonerId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeagueSummonerRepository extends JpaRepository<LeagueSummonerEntity, LeagueSummonerId> {

    List<LeagueSummonerEntity> findAllByPuuid(String puuid);
}
