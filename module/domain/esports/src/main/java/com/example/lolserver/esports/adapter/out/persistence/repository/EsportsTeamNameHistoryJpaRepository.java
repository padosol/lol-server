package com.example.lolserver.esports.adapter.out.persistence.repository;

import com.example.lolserver.esports.adapter.out.persistence.entity.EsportsTeamNameHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EsportsTeamNameHistoryJpaRepository extends JpaRepository<EsportsTeamNameHistoryEntity, Long> {

    Optional<EsportsTeamNameHistoryEntity> findByHistoryIdAndTeamId(Long historyId, String teamId);
}
