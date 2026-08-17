package com.example.lolserver.community.application.model.readmodel;

import com.example.lolserver.community.domain.Post;
import com.example.lolserver.community.domain.Vote;
import com.example.lolserver.community.domain.vo.VoteType;
import com.example.lolserver.member.application.model.readmodel.MemberProfileReadModel;
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
    private final Long categoryId;
    private final int viewCount;
    private final int upvoteCount;
    private final int downvoteCount;
    private final int commentCount;
    private final AuthorReadModel author;
    private final VoteType currentUserVote;
    private final boolean currentUserBookmarked;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public static PostDetailReadModel of(Post post, MemberProfileReadModel author, Vote currentUserVote,
                                         boolean currentUserBookmarked) {
        return PostDetailReadModel.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .categoryId(post.getCategoryId())
                .viewCount(post.getViewCount())
                .upvoteCount(post.getUpvoteCount())
                .downvoteCount(post.getDownvoteCount())
                .commentCount(post.getCommentCount())
                .author(AuthorReadModel.of(author))
                .currentUserVote(
                        currentUserVote != null
                                ? currentUserVote.getVoteType() : null)
                .currentUserBookmarked(currentUserBookmarked)
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }
}
