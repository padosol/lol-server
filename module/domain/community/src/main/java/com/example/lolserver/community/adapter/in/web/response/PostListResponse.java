package com.example.lolserver.community.adapter.in.web.response;

import com.example.lolserver.community.application.model.PostListReadModel;

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
                readModel.getAuthor() != null ? AuthorResponse.from(readModel.getAuthor()) : null,
                readModel.getCreatedAt()
        );
    }
}
