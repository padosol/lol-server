package com.example.lolserver.esports.adapter.out.persistence.repository;

import com.example.lolserver.esports.adapter.out.persistence.entity.EsportsLeagueEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EsportsLeagueJpaRepository extends JpaRepository<EsportsLeagueEntity, String> {
}
