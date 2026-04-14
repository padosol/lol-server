package com.example.lolserver.domain.community.application.model;

import com.example.lolserver.domain.community.domain.Comment;
import com.example.lolserver.domain.member.domain.Member;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class CommentTreeReadModel {

    private static final String DELETED_COMMENT_CONTENT = "[삭제된 댓글입니다]";

    private final Long id;
    private final Long postId;
    private final Long parentCommentId;
    private final String content;
    private final int depth;
    private final int upvoteCount;
    private final int downvoteCount;
    private final boolean deleted;
    private final AuthorReadModel author;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    @Builder.Default
    private List<CommentTreeReadModel> children = new ArrayList<>();

    public static CommentTreeReadModel of(Comment comment, Member member) {
        AuthorReadModel author = member != null
                ? AuthorReadModel.of(member) : null;

        String content = comment.isDeleted()
                ? DELETED_COMMENT_CONTENT : comment.getContent();

        return CommentTreeReadModel.builder()
                .id(comment.getId())
                .postId(comment.getPostId())
                .parentCommentId(comment.getParentCommentId())
                .content(content)
                .depth(comment.getDepth())
                .upvoteCount(comment.getUpvoteCount())
                .downvoteCount(comment.getDownvoteCount())
                .deleted(comment.isDeleted())
                .author(author)
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .children(new ArrayList<>())
                .build();
    }
}
