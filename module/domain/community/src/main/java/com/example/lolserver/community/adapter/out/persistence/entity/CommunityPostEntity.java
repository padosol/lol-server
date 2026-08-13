package com.example.lolserver.community.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "community_post")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityPostEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * 소속 게시판({@code community_category.id}).
     *
     * <p>표시용 {@code code} 문자열이 아니라 대리키를 들고 있다. code 는 라벨 성격이라
     * 바뀔 수 있고, 바뀌는 값을 FK 로 삼으면 참조 무결성이 리네임 자체를 막는다(V33).
     * 도메인/API 가 쓰는 code 로의 변환은 {@code CategoryCodeResolver} 가 맡는다.
     *
     * <p>{@code @ManyToOne} 을 걸지 않는 이유는 카테고리 엔티티 쪽과 같다 — 목록 조회가
     * 필요로 하는 것은 code 한 컬럼뿐이라 연관을 타고 엔티티를 적재할 이유가 없다.
     */
    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(name = "view_count", nullable = false)
    private int viewCount;

    @Column(name = "upvote_count", nullable = false)
    private int upvoteCount;

    @Column(name = "downvote_count", nullable = false)
    private int downvoteCount;

    @Column(name = "comment_count", nullable = false)
    private int commentCount;

    @Column(name = "hot_score", nullable = false)
    private double hotScore;

    @Column(nullable = false)
    private boolean deleted;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
