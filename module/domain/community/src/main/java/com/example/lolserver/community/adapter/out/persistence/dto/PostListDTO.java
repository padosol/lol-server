package com.example.lolserver.community.adapter.out.persistence.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 목록 프로젝션. 카테고리는 저장된 그대로 id 이며 응답까지 id 로 나간다 —
 * 라벨 해석은 클라이언트가 카테고리 트리로 하므로 게시글 조회에 조인이 필요 없다.
 */
@Getter
@NoArgsConstructor
public class PostListDTO {

    private Long id;
    private String title;
    private Long categoryId;
    private int viewCount;
    private int upvoteCount;
    private int downvoteCount;
    private int commentCount;
    private double hotScore;
    private LocalDateTime createdAt;
    private Long memberId;

    @QueryProjection
    public PostListDTO(Long id, String title, Long categoryId, int viewCount,
                       int upvoteCount, int downvoteCount, int commentCount,
                       double hotScore, LocalDateTime createdAt, Long memberId) {
        this.id = id;
        this.title = title;
        this.categoryId = categoryId;
        this.viewCount = viewCount;
        this.upvoteCount = upvoteCount;
        this.downvoteCount = downvoteCount;
        this.commentCount = commentCount;
        this.hotScore = hotScore;
        this.createdAt = createdAt;
        this.memberId = memberId;
    }
}
