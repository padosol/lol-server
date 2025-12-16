package com.example.lolserver.repository.league;

import com.example.lolserver.repository.league.entity.LeagueSummonerHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeagueSummonerHistoryRepository extends JpaRepository<LeagueSummonerHistoryEntity, Long> {
    List<LeagueSummonerHistoryEntity> findAllByLeagueSummonerIdInOrderByCreatedAtDesc(List<Long> leagueSummonerIds);
}
