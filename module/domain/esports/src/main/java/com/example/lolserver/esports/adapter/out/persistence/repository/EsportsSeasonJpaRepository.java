package com.example.lolserver.esports.adapter.out.persistence.repository;

import com.example.lolserver.esports.adapter.out.persistence.entity.EsportsSeasonEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EsportsSeasonJpaRepository extends JpaRepository<EsportsSeasonEntity, String> {
}
