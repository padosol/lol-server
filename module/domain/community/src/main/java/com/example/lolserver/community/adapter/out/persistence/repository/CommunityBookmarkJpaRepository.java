package com.example.lolserver.community.adapter.out.persistence.repository;

import com.example.lolserver.community.adapter.out.persistence.entity.CommunityBookmarkEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CommunityBookmarkJpaRepository
        extends JpaRepository<CommunityBookmarkEntity, Long> {

    Optional<CommunityBookmarkEntity> findByMemberIdAndPostId(Long memberId, Long postId);
}
