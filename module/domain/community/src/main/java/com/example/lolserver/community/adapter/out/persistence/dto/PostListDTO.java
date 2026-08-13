package com.example.lolserver.community.adapter.out.persistence.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 목록 프로젝션. 카테고리는 저장된 그대로 id 이고, 응답이 쓰는 code 로는 어댑터가 옮긴다 —
 * 조인해서 code 를 직접 뽑지 않는 이유는 8행짜리 테이블을 페이지마다 조인하느니
 * 맵 한 번 받아 메모리에서 맞추는 편이 단순하고, 정렬/인덱스 계약도 건드리지 않기 때문이다.
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
