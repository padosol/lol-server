package com.example.lolserver.repository.summoner.repository;

import com.example.lolserver.repository.summoner.entity.SummonerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SummonerJpaRepository extends JpaRepository<SummonerEntity, String> {
}
