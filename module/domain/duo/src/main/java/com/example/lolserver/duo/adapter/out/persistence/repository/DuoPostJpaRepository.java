package com.example.lolserver.duo.adapter.out.persistence.repository;

import com.example.lolserver.duo.adapter.out.persistence.entity.DuoPostEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface DuoPostJpaRepository extends JpaRepository<DuoPostEntity, Long> {

    Slice<DuoPostEntity> findByMemberIdOrderByCreatedAtDesc(Long memberId, Pageable pageable);

    boolean existsByMemberIdAndStatusAndExpiresAtAfter(Long memberId, String status,
            LocalDateTime now);

    @Query("SELECT p.id FROM DuoPostEntity p "
            + "WHERE p.status = :status AND p.expiresAt <= :now")
    List<Long> findIdsByStatusAndExpiresAtBefore(@Param("status") String status,
            @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE DuoPostEntity p SET p.status = :newStatus, p.updatedAt = CURRENT_TIMESTAMP "
            + "WHERE p.id IN :ids AND p.status = :expectedStatus")
    void updateStatusByIds(@Param("ids") List<Long> ids, @Param("newStatus") String newStatus,
            @Param("expectedStatus") String expectedStatus);

    @Modifying
    @Query("UPDATE DuoPostEntity p SET p.status = 'MATCHED', p.updatedAt = CURRENT_TIMESTAMP "
            + "WHERE p.id = :id AND p.status = 'ACTIVE' AND p.expiresAt > CURRENT_TIMESTAMP")
    int markMatchedIfActive(@Param("id") Long id);
}
