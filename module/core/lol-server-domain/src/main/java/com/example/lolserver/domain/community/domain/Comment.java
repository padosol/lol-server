package com.example.lolserver.domain.community.domain;

import com.example.lolserver.support.error.CoreException;
import com.example.lolserver.support.error.ErrorType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Comment {

    private Long id;
    private Long postId;
    private Long memberId;
    private Long parentCommentId;
    private String content;
    private int depth;
    private int upvoteCount;
    private int downvoteCount;
    private boolean deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static Comment create(Long postId, Long memberId, String content,
                                  Long parentCommentId, int depth) {
        return Comment.builder()
                .postId(postId)
                .memberId(memberId)
                .content(content)
                .parentCommentId(parentCommentId)
                .depth(depth)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public boolean isOwner(Long memberId) {
        return this.memberId.equals(memberId);
    }

    public void validateOwner(Long memberId) {
        if (!this.memberId.equals(memberId)) {
            throw new CoreException(ErrorType.FORBIDDEN);
        }
    }

    public void markDeleted() {
        this.deleted = true;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateContent(String content) {
        this.content = content;
        this.updatedAt = LocalDateTime.now();
    }
}
