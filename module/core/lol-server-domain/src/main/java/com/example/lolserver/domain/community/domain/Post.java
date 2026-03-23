package com.example.lolserver.domain.community.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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
