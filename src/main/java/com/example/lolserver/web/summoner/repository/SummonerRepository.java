package com.example.lolserver.web.summoner.repository;

import com.example.lolserver.web.summoner.entity.Summoner;
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


    @Query("select s from Summoner s where Function('replace', s.gameName, ' ', '') = Function('replace', :gameName, ' ', '')" +
            "                          and Function('replace', s.tagLine, ' ', '') = Function('replace', :tagLine, ' ', '')")
    List<Summoner> findAllByGameNameAndTagLine(String gameName, String tagLine);

    @Query("select s from Summoner s where Function('replace', s.name, ' ', '') = Function('replace', :name, ' ', '') or Function('replace', s.gameName, ' ', '') = Function('replace', :name, ' ', '')")
    List<Summoner> findAllByGameName(String name);


}
