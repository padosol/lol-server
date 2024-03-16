package com.example.lolserver.web.repository;

import com.example.lolserver.entity.summoner.Summoner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SummonerRepository extends JpaRepository<Summoner, String> {



    Optional<Summoner> findSummonerByName(String name);

    @Query("select s from Summoner s where s.tagLine = :tagLine and Function('replace', s.gameName, ' ', '') like %:gameName%")
    Optional<Summoner> findSummonerByGameNameAndTagLine(String gameName, String tagLine);

    Optional<Summoner> findSummonerById(String id);

    Optional<Summoner> findSummonerByPuuid(String puuid);


    List<Summoner> findAllByGameNameAndTagLine(String gameName, String tagLine);


}
