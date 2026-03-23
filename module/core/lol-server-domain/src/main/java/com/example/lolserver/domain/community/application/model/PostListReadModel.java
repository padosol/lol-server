package com.example.lolserver.domain.community.application.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class PostListReadModel {

    private final Long id;
    private final String title;
    private final String category;
    private final int viewCount;
    private final int upvoteCount;
    private final int downvoteCount;
    private final int commentCount;
    private final double hotScore;
    private final AuthorReadModel author;
    private final LocalDateTime createdAt;
}
