package com.example.lolserver.duo.adapter.out.persistence.repository;

import com.example.lolserver.duo.adapter.out.persistence.entity.DuoRequestEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DuoRequestJpaRepository extends JpaRepository<DuoRequestEntity, Long> {

    List<DuoRequestEntity> findByDuoPostId(Long duoPostId);

    boolean existsByDuoPostIdAndRequesterIdAndStatusIn(Long duoPostId,
            Long requesterId, List<String> statuses);

    @Modifying
    @Query("UPDATE DuoRequestEntity r SET r.status = 'REJECTED', r.updatedAt = CURRENT_TIMESTAMP "
            + "WHERE r.duoPostId = :duoPostId AND r.id != :excludeRequestId "
            + "AND r.status IN ('PENDING', 'ACCEPTED')")
    void rejectAllPendingAndAccepted(@Param("duoPostId") Long duoPostId,
            @Param("excludeRequestId") Long excludeRequestId);

    Slice<DuoRequestEntity> findByRequesterIdOrderByCreatedAtDesc(Long requesterId,
            Pageable pageable);
}
