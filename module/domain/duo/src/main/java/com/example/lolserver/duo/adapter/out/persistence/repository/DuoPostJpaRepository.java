package com.example.lolserver.duo.adapter.out.persistence.repository;

import com.example.lolserver.duo.adapter.out.persistence.entity.DuoPostEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DuoPostJpaRepository extends JpaRepository<DuoPostEntity, Long> {

    Slice<DuoPostEntity> findByMemberIdOrderByCreatedAtDesc(Long memberId, Pageable pageable);
}
