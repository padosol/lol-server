package com.example.lolserver.community.application.model.readmodel;

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
    /**
     * 현재 이 글에 붙어 있는 이미지. 본문에 URL 이 이미 들어 있어 렌더링에는 불필요하지만,
     * 수정 화면이 전체 교체 시맨틱({@code imageIds})을 채우려면 이 목록이 필요하다.
     */
    private final List<PostImageReadModel> images;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public static PostDetailReadModel of(Post post, MemberProfileReadModel author, Vote currentUserVote,
                                         boolean currentUserBookmarked) {
        return of(post, author, currentUserVote, currentUserBookmarked, List.of());
    }

    public static PostDetailReadModel of(Post post, MemberProfileReadModel author, Vote currentUserVote,
                                         boolean currentUserBookmarked, List<PostImageReadModel> images) {
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
                .images(images)
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }
}
