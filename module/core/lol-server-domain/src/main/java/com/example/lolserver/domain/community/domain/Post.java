package com.example.lolserver.domain.community.domain;

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
    private String category;
    private int viewCount;
    private int upvoteCount;
    private int downvoteCount;
    private int commentCount;
    private double hotScore;
    private boolean deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static Post create(Long memberId, String title, String content, String category) {
        Post post = Post.builder()
                .memberId(memberId)
                .title(title)
                .content(content)
                .category(category)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        post.calculateHotScore();
        return post;
    }

    public boolean isOwner(Long memberId) {
        return this.memberId.equals(memberId);
    }

    public void markDeleted() {
        this.deleted = true;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateContent(String title, String content, String category) {
        this.title = title;
        this.content = content;
        this.category = category;
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
