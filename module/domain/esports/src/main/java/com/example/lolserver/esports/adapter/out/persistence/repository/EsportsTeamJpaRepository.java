package com.example.lolserver.esports.adapter.out.persistence.repository;

import com.example.lolserver.esports.adapter.out.persistence.entity.EsportsTeamEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EsportsTeamJpaRepository extends JpaRepository<EsportsTeamEntity, String> {

    boolean existsByParentTeamId(String parentTeamId);
}
