package com.example.lolserver.controller.community.response;

import com.example.lolserver.domain.community.application.model.PostDetailReadModel;

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
}
