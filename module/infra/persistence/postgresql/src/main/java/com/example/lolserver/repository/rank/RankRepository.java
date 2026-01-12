package com.example.lolserver.repository.rank;

import com.example.lolserver.QueueType;
import com.example.lolserver.repository.rank.entity.RankEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RankRepository extends JpaRepository<RankEntity, Long> {
    List<RankEntity> findByQueueType(QueueType queueType);
}
