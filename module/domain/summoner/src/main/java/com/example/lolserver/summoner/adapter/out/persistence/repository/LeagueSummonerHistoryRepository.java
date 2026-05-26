package com.example.lolserver.summoner.adapter.out.persistence.repository;

import com.example.lolserver.summoner.adapter.out.persistence.entity.LeagueSummonerHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeagueSummonerHistoryRepository extends JpaRepository<LeagueSummonerHistoryEntity, Long> {
    List<LeagueSummonerHistoryEntity> findAllByLeagueSummonerIdInOrderByCreatedAtDesc(List<Long> leagueSummonerIds);
}
