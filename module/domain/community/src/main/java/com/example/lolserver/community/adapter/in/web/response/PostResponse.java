package com.example.lolserver.community.adapter.in.web.response;

import com.example.lolserver.community.application.model.readmodel.PostDetailReadModel;
import com.example.lolserver.community.application.model.resultmodel.PostDetailResultModel;

import java.time.LocalDateTime;

public record PostResponse(
        Long id,
        String title,
        String content,
        Long categoryId,
        int viewCount,
        int upvoteCount,
        int downvoteCount,
        int commentCount,
        AuthorResponse author,
        String currentUserVote,
        boolean currentUserBookmarked,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PostResponse from(PostDetailReadModel readModel) {
        return new PostResponse(
                readModel.getId(),
                readModel.getTitle(),
                readModel.getContent(),
                readModel.getCategoryId(),
                readModel.getViewCount(),
                readModel.getUpvoteCount(),
                readModel.getDownvoteCount(),
                readModel.getCommentCount(),
                AuthorResponse.from(readModel.getAuthor()),
                readModel.getCurrentUserVote() != null ? readModel.getCurrentUserVote().name() : null,
                readModel.isCurrentUserBookmarked(),
                readModel.getCreatedAt(),
                readModel.getUpdatedAt()
        );
    }

    public static PostResponse from(PostDetailResultModel resultModel) {
        return new PostResponse(
                resultModel.getId(),
                resultModel.getTitle(),
                resultModel.getContent(),
                resultModel.getCategoryId(),
                resultModel.getViewCount(),
                resultModel.getUpvoteCount(),
                resultModel.getDownvoteCount(),
                resultModel.getCommentCount(),
                AuthorResponse.from(resultModel.getAuthor()),
                resultModel.getCurrentUserVote() != null ? resultModel.getCurrentUserVote().name() : null,
                resultModel.isCurrentUserBookmarked(),
                resultModel.getCreatedAt(),
                resultModel.getUpdatedAt()
        );
    }
}
