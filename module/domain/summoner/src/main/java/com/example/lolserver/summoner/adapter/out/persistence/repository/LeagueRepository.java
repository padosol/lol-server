package com.example.lolserver.summoner.adapter.out.persistence.repository;

import com.example.lolserver.summoner.adapter.out.persistence.entity.LeagueEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LeagueRepository extends JpaRepository<LeagueEntity, String> {
}
