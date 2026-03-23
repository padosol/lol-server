package com.example.lolserver.domain.community.application.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class CommentTreeReadModel {

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

    @Setter
    @Builder.Default
    private List<CommentTreeReadModel> children = new ArrayList<>();
}
