package com.example.lolserver.web.summoner.repository;

import com.example.lolserver.web.summoner.entity.Summoner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SummonerJpaRepository extends JpaRepository<Summoner, String> {

    Optional<Summoner> findSummonerByPuuid(String puuid);

}
