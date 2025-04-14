package com.example.lolserver.domain.summoner.domain.repository;

import com.example.lolserver.domain.summoner.domain.entity.Summoner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SummonerJpaRepository extends JpaRepository<Summoner, String> {

    Optional<Summoner> findSummonerByPuuid(String puuid);

}
