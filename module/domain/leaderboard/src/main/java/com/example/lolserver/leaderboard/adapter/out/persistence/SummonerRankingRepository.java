package com.example.lolserver.leaderboard.adapter.out.persistence;

import com.example.lolserver.leaderboard.adapter.out.persistence.entity.SummonerRankingEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SummonerRankingRepository extends JpaRepository<SummonerRankingEntity, Long> {
    List<SummonerRankingEntity> findByQueue(String queue);

    Page<SummonerRankingEntity> findByPlatformIdAndQueue(String platformId, String queue, Pageable pageable);

    Page<SummonerRankingEntity> findByPlatformIdAndQueueAndTier(
            String platformId, String queue, String tier, Pageable pageable);
}
