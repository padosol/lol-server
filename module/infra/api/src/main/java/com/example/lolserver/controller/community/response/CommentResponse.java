package com.example.lolserver.controller.community.response;

import com.example.lolserver.domain.community.application.model.CommentTreeReadModel;

import java.time.LocalDateTime;
import java.util.List;

public record CommentResponse(
        Long id,
        Long postId,
        Long parentCommentId,
        String content,
        int depth,
        int upvoteCount,
        int downvoteCount,
        boolean deleted,
        AuthorResponse author,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<CommentResponse> children
) {
    public static CommentResponse from(CommentTreeReadModel readModel) {
        List<CommentResponse> childResponses = readModel.getChildren() != null
                ? readModel.getChildren().stream().map(CommentResponse::from).toList()
                : List.of();

        return new CommentResponse(
                readModel.getId(),
                readModel.getPostId(),
                readModel.getParentCommentId(),
                readModel.getContent(),
                readModel.getDepth(),
                readModel.getUpvoteCount(),
                readModel.getDownvoteCount(),
                readModel.isDeleted(),
                AuthorResponse.from(readModel.getAuthor()),
                readModel.getCreatedAt(),
                readModel.getUpdatedAt(),
                childResponses
        );
    }
}
