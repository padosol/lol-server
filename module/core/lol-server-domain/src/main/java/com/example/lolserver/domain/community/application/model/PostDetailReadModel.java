package com.example.lolserver.domain.community.application.model;

import com.example.lolserver.domain.community.domain.Post;
import com.example.lolserver.domain.community.domain.Vote;
import com.example.lolserver.domain.community.domain.vo.VoteType;
import com.example.lolserver.domain.member.domain.Member;
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

    public static PostDetailReadModel of(Post post, Member member, Vote currentUserVote) {
        return PostDetailReadModel.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .category(post.getCategory())
                .viewCount(post.getViewCount())
                .upvoteCount(post.getUpvoteCount())
                .downvoteCount(post.getDownvoteCount())
                .commentCount(post.getCommentCount())
                .author(AuthorReadModel.of(member))
                .currentUserVote(
                        currentUserVote != null
                                ? currentUserVote.getVoteType() : null)
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }
}
