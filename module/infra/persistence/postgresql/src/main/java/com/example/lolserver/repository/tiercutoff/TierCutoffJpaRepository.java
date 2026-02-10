package com.example.lolserver.repository.tiercutoff;

import com.example.lolserver.repository.tiercutoff.entity.TierCutoffEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TierCutoffJpaRepository extends JpaRepository<TierCutoffEntity, Long> {

    Optional<TierCutoffEntity> findByQueueAndTierAndRegion(String queue, String tier, String region);

    List<TierCutoffEntity> findAllByRegion(String region);

    List<TierCutoffEntity> findByRegionAndQueue(String region, String queue);
}
