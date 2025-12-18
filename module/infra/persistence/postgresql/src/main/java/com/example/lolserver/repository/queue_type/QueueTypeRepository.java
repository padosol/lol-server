package com.example.lolserver.repository.queue_type;

import com.example.lolserver.repository.queue_type.entity.QueueEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QueueTypeRepository extends JpaRepository<QueueEntity, Long> {
    List<QueueEntity> findAllByIsTab(boolean isTab);
}
