package com.example.lolserver.repository.community.repository;

import com.example.lolserver.repository.community.entity.CommunityCommentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommunityCommentJpaRepository extends JpaRepository<CommunityCommentEntity, Long> {

    List<CommunityCommentEntity> findByPostIdOrderByCreatedAtAsc(Long postId);

    int countByPostIdAndDeletedFalse(Long postId);

    @Modifying
    @Query("UPDATE CommunityCommentEntity c "
            + "SET c.upvoteCount = :upvoteCount, "
            + "c.downvoteCount = :downvoteCount "
            + "WHERE c.id = :commentId")
    void updateVoteCounts(
            @Param("commentId") Long commentId,
            @Param("upvoteCount") int upvoteCount,
            @Param("downvoteCount") int downvoteCount);
}
