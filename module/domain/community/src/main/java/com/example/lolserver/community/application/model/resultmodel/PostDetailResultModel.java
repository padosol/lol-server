package com.example.lolserver.community.application.model.resultmodel;

import com.example.lolserver.community.application.model.readmodel.AuthorReadModel;
import com.example.lolserver.community.application.model.readmodel.PostImageReadModel;
import com.example.lolserver.community.domain.Post;
import com.example.lolserver.community.domain.Vote;
import com.example.lolserver.community.domain.vo.VoteType;
import com.example.lolserver.member.application.model.readmodel.MemberProfileReadModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class PostDetailResultModel {

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
    /** 방금 첨부가 확정된 이미지 목록. 생성·수정 응답이 곧바로 수정 화면을 채울 수 있게 한다. */
    private final List<PostImageReadModel> images;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public static PostDetailResultModel of(Post post, MemberProfileReadModel author, Vote currentUserVote,
                                           boolean currentUserBookmarked) {
        return of(post, author, currentUserVote, currentUserBookmarked, List.of());
    }

    public static PostDetailResultModel of(Post post, MemberProfileReadModel author, Vote currentUserVote,
                                           boolean currentUserBookmarked, List<PostImageReadModel> images) {
        return PostDetailResultModel.builder()
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
                .images(images)
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }
}
