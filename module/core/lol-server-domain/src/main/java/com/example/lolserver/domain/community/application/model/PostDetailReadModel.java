package com.example.lolserver.domain.community.application.model;

import com.example.lolserver.domain.community.domain.vo.VoteType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class PostDetailReadModel {

    private final Long id;
    private final String title;
    private final String content;
    private final String category;
    private final int viewCount;
    private final int upvoteCount;
    private final int downvoteCount;
    private final int commentCount;
    private final AuthorReadModel author;
    private final VoteType currentUserVote;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
}
