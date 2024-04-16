package com.example.lolserver.web.summoner.repository;

import com.example.lolserver.web.summoner.entity.Summoner;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SummonerRepositoryV2 extends JpaRepository<Summoner, String> {
}
