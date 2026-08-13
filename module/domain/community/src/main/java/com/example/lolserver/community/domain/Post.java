package com.example.lolserver.community.domain;

import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Post {

    private Long id;
    private Long memberId;
    private String title;
    private String content;
    /** 소속 게시판 식별자. 클라이언트도 이 값을 그대로 주고받는다(카테고리 트리 응답의 id). */
    private Long categoryId;
    private int viewCount;
    private int upvoteCount;
    private int downvoteCount;
    private int commentCount;
    private double hotScore;
    private boolean deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static Post create(Long memberId, String title, String content, Long categoryId) {
        Post post = Post.builder()
                .memberId(memberId)
                .title(title)
                .content(content)
                .categoryId(categoryId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        post.calculateHotScore();
        return post;
    }

    public boolean isOwner(Long memberId) {
        return this.memberId.equals(memberId);
    }

    public void validateOwner(Long memberId) {
        if (!this.memberId.equals(memberId)) {
            throw new CoreException(ErrorType.FORBIDDEN);
        }
    }

    public void validateNotDeleted() {
        if (this.deleted) {
            throw new CoreException(ErrorType.POST_NOT_FOUND);
        }
    }

    public void markDeleted() {
        this.deleted = true;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateContent(String title, String content, Long categoryId) {
        this.title = title;
        this.content = content;
        this.categoryId = categoryId;
        this.updatedAt = LocalDateTime.now();
    }

    public void incrementViewCount() {
        this.viewCount++;
    }

    public void applyVoteCounts(int upvoteCount, int downvoteCount) {
        this.upvoteCount = upvoteCount;
        this.downvoteCount = downvoteCount;
        calculateHotScore();
    }

    public double calculateHotScore() {
        int score = upvoteCount - downvoteCount;
        double order = Math.log10(Math.max(Math.abs(score), 1));
        int sign = Integer.compare(score, 0);
        long epoch = createdAt.toEpochSecond(ZoneOffset.UTC);
        long reference = 1735689600L; // 2025-01-01 UTC
        this.hotScore = sign * order + (epoch - reference) / 45000.0;
        return this.hotScore;
    }
}
