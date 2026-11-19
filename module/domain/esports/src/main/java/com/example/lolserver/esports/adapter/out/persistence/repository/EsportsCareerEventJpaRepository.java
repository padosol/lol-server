package com.example.lolserver.esports.adapter.out.persistence.repository;

import com.example.lolserver.esports.adapter.out.persistence.entity.EsportsCareerEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EsportsCareerEventJpaRepository extends JpaRepository<EsportsCareerEventEntity, Long> {

    List<EsportsCareerEventEntity> findByPersonIdOrderByEventDateAscEventIdAsc(String personId);

    boolean existsByPersonId(String personId);
}
