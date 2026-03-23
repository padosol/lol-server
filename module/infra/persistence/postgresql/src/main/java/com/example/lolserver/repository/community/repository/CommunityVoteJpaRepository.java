package com.example.lolserver.repository.community.repository;

import com.example.lolserver.repository.community.entity.CommunityVoteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CommunityVoteJpaRepository extends JpaRepository<CommunityVoteEntity, Long> {

    Optional<CommunityVoteEntity> findByMemberIdAndTargetTypeAndTargetId(
            Long memberId, String targetType, Long targetId);

    int countByTargetTypeAndTargetIdAndVoteType(String targetType, Long targetId, String voteType);
}
