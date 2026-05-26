package com.example.lolserver.community.adapter.out.persistence.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class PostListDTO {

    private Long id;
    private String title;
    private String category;
    private int viewCount;
    private int upvoteCount;
    private int downvoteCount;
    private int commentCount;
    private double hotScore;
    private LocalDateTime createdAt;
    private Long memberId;

    @QueryProjection
    public PostListDTO(Long id, String title, String category, int viewCount,
                       int upvoteCount, int downvoteCount, int commentCount,
                       double hotScore, LocalDateTime createdAt, Long memberId) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.viewCount = viewCount;
        this.upvoteCount = upvoteCount;
        this.downvoteCount = downvoteCount;
        this.commentCount = commentCount;
        this.hotScore = hotScore;
        this.createdAt = createdAt;
        this.memberId = memberId;
    }
}
