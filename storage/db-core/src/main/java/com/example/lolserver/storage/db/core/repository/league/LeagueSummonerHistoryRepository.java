package com.example.lolserver.storage.db.core.repository.league;

import com.example.lolserver.storage.db.core.repository.league.entity.LeagueSummonerHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeagueSummonerHistoryRepository extends JpaRepository<LeagueSummonerHistory, Long> {
    List<LeagueSummonerHistory> findAllByLeagueSummonerIdInOrderByCreatedAtDesc(List<Long> leagueSummonerIds);
}
