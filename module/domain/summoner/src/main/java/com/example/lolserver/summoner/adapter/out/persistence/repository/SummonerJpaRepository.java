package com.example.lolserver.summoner.adapter.out.persistence.repository;

import com.example.lolserver.summoner.adapter.out.persistence.entity.SummonerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface SummonerJpaRepository extends JpaRepository<SummonerEntity, String> {
    List<SummonerEntity> findAllByPuuidIn(Collection<String> puuids);
}
