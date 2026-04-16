package com.example.lolserver.controller.community.response;

import com.example.lolserver.domain.community.application.model.PostListReadModel;

import java.time.LocalDateTime;

public record PostListResponse(
        Long id,
        String title,
        String category,
        int viewCount,
        int upvoteCount,
        int downvoteCount,
        int commentCount,
        double hotScore,
        AuthorResponse author,
        LocalDateTime createdAt
) {
    public static PostListResponse from(PostListReadModel readModel) {
        return new PostListResponse(
                readModel.getId(),
                readModel.getTitle(),
                readModel.getCategory(),
                readModel.getViewCount(),
                readModel.getUpvoteCount(),
                readModel.getDownvoteCount(),
                readModel.getCommentCount(),
                readModel.getHotScore(),
                AuthorResponse.from(readModel.getAuthor()),
                readModel.getCreatedAt()
        );
    }
}
