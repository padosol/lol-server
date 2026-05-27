package com.example.lolserver.community.adapter.in.web.response;

import com.example.lolserver.community.application.model.readmodel.PostDetailReadModel;
import com.example.lolserver.community.application.model.resultmodel.PostDetailResultModel;

import java.time.LocalDateTime;

public record PostResponse(
        Long id,
        String title,
        String content,
        String category,
        int viewCount,
        int upvoteCount,
        int downvoteCount,
        int commentCount,
        AuthorResponse author,
        String currentUserVote,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PostResponse from(PostDetailReadModel readModel) {
        return new PostResponse(
                readModel.getId(),
                readModel.getTitle(),
                readModel.getContent(),
                readModel.getCategory(),
                readModel.getViewCount(),
                readModel.getUpvoteCount(),
                readModel.getDownvoteCount(),
                readModel.getCommentCount(),
                AuthorResponse.from(readModel.getAuthor()),
                readModel.getCurrentUserVote() != null ? readModel.getCurrentUserVote().name() : null,
                readModel.getCreatedAt(),
                readModel.getUpdatedAt()
        );
    }

    public static PostResponse from(PostDetailResultModel resultModel) {
        return new PostResponse(
                resultModel.getId(),
                resultModel.getTitle(),
                resultModel.getContent(),
                resultModel.getCategory(),
                resultModel.getViewCount(),
                resultModel.getUpvoteCount(),
                resultModel.getDownvoteCount(),
                resultModel.getCommentCount(),
                AuthorResponse.from(resultModel.getAuthor()),
                resultModel.getCurrentUserVote() != null ? resultModel.getCurrentUserVote().name() : null,
                resultModel.getCreatedAt(),
                resultModel.getUpdatedAt()
        );
    }
}
