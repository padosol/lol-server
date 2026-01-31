package com.example.lolserver.repository.rank;

import com.example.lolserver.repository.rank.entity.SummonerRankingEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SummonerRankingRepository extends JpaRepository<SummonerRankingEntity, Long> {
    List<SummonerRankingEntity> findByQueue(String queue);

    Page<SummonerRankingEntity> findByQueue(String queue, Pageable pageable);

    Page<SummonerRankingEntity> findByQueueAndTier(String queue, String tier, Pageable pageable);
}
