package com.example.lolserver.gamedata.adapter.out.persistence;

import com.example.lolserver.gamedata.adapter.out.persistence.entity.QueueEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QueueTypeRepository extends JpaRepository<QueueEntity, Long> {
    List<QueueEntity> findAllByIsTab(boolean isTab);
}
