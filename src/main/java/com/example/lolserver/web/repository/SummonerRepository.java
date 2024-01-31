package com.example.lolserver.web.repository;

import com.example.lolserver.entity.summoner.Summoner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SummonerRepository extends JpaRepository<Summoner, String> {

    Optional<Summoner> findSummonerByName(String name);
}
