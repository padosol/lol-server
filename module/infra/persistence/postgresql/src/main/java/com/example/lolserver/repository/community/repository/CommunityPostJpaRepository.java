package com.example.lolserver.repository.community.repository;

import com.example.lolserver.repository.community.entity.CommunityPostEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommunityPostJpaRepository extends JpaRepository<CommunityPostEntity, Long> {

    Slice<CommunityPostEntity> findByMemberIdAndDeletedFalseOrderByCreatedAtDesc(Long memberId, Pageable pageable);

    @Modifying
    @Query("UPDATE CommunityPostEntity p SET p.viewCount = p.viewCount + 1 WHERE p.id = :postId")
    void incrementViewCount(@Param("postId") Long postId);

    @Modifying
    @Query("UPDATE CommunityPostEntity p SET p.upvoteCount = :upvoteCount, "
            + "p.downvoteCount = :downvoteCount WHERE p.id = :postId")
    void updateVoteCounts(@Param("postId") Long postId,
                          @Param("upvoteCount") int upvoteCount,
                          @Param("downvoteCount") int downvoteCount);

    @Modifying
    @Query("UPDATE CommunityPostEntity p SET p.commentCount = :commentCount WHERE p.id = :postId")
    void updateCommentCount(@Param("postId") Long postId, @Param("commentCount") int commentCount);

    @Modifying
    @Query("UPDATE CommunityPostEntity p SET p.hotScore = :hotScore WHERE p.id = :postId")
    void updateHotScore(@Param("postId") Long postId, @Param("hotScore") double hotScore);
}
