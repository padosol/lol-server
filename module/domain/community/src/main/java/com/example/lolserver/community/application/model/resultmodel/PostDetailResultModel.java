package com.example.lolserver.community.application.model.resultmodel;

import com.example.lolserver.community.application.model.readmodel.AuthorReadModel;
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
public class PostDetailResultModel {

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
    private final boolean currentUserBookmarked;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public static PostDetailResultModel of(Post post, MemberProfileReadModel author, Vote currentUserVote,
                                           boolean currentUserBookmarked) {
        return PostDetailResultModel.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .category(post.getCategory())
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
