package com.example.lolserver.community.adapter.out.persistence.repository;

import com.example.lolserver.community.adapter.out.persistence.dto.PostListDTO;
import com.example.lolserver.community.adapter.out.persistence.entity.CommunityBookmarkEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CommunityBookmarkJpaRepository
        extends JpaRepository<CommunityBookmarkEntity, Long> {

    Optional<CommunityBookmarkEntity> findByMemberIdAndPostId(Long memberId, Long postId);

    boolean existsByMemberIdAndPostId(Long memberId, Long postId);

    /**
     * 북마크한 게시글 목록.
     *
     * <p>정렬 기준은 글 작성 시각이 아니라 <b>북마크한 시각</b>이다 — 사용자가 기대하는 순서는
     * "내가 최근에 담은 순"이고, 이를 위해 V31 의 idx_cb_member_created 가 있다.
     * 같은 시각의 북마크가 있을 때 페이지 경계에서 행이 중복/누락되지 않도록 b.id 를 타이브레이커로 둔다.
     *
     * <p>엔티티가 아니라 DTO 로 프로젝션하는 이유: 목록 응답은 본문(content, TEXT)을 쓰지 않는다.
     * 엔티티를 가져오면 페이지당 20건의 본문을 통째로 읽어 영속성 컨텍스트에 적재한다.
     *
     * <p>삭제된 글은 목록에서 제외하되 북마크 레코드 자체는 남긴다.
     */
    @Query("""
            SELECT new com.example.lolserver.community.adapter.out.persistence.dto.PostListDTO(
                p.id, p.title, p.categoryId, p.viewCount, p.upvoteCount, p.downvoteCount,
                p.commentCount, p.hotScore, p.createdAt, p.memberId)
            FROM CommunityPostEntity p
            JOIN CommunityBookmarkEntity b ON b.postId = p.id
            WHERE b.memberId = :memberId AND p.deleted = false
            ORDER BY b.createdAt DESC, b.id DESC
            """)
    Slice<PostListDTO> findBookmarkedPosts(@Param("memberId") Long memberId, Pageable pageable);
}
