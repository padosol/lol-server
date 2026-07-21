package com.example.lolserver.community.adapter.out.persistence.repository;

import com.example.lolserver.community.adapter.out.persistence.entity.CommunityBookmarkEntity;
import com.example.lolserver.community.adapter.out.persistence.entity.CommunityPostEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CommunityBookmarkJpaRepository
        extends JpaRepository<CommunityBookmarkEntity, Long> {

    Optional<CommunityBookmarkEntity> findByMemberIdAndPostId(Long memberId, Long postId);

    /**
     * 북마크한 게시글 목록.
     * 정렬 기준은 글 작성 시각이 아니라 <b>북마크한 시각</b>이다 — 사용자가 기대하는 순서는
     * "내가 최근에 담은 순"이다. 그래서 idx_cb_member_created 가 필요하다.
     * 삭제된 글은 목록에서 제외한다 (북마크 레코드 자체는 남는다).
     */
    @Query("""
            SELECT p FROM CommunityPostEntity p
            JOIN CommunityBookmarkEntity b ON b.postId = p.id
            WHERE b.memberId = :memberId AND p.deleted = false
            ORDER BY b.createdAt DESC
            """)
    Slice<CommunityPostEntity> findBookmarkedPosts(@Param("memberId") Long memberId,
                                                   Pageable pageable);
}
