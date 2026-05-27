package com.example.lolserver.community.adapter.in.web.response;

import com.example.lolserver.community.application.model.readmodel.CommentTreeReadModel;
import com.example.lolserver.community.application.model.resultmodel.CommentTreeResultModel;

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

    public static CommentResponse from(CommentTreeResultModel resultModel) {
        List<CommentResponse> childResponses = resultModel.getChildren() != null
                ? resultModel.getChildren().stream().map(CommentResponse::from).toList()
                : List.of();

        return new CommentResponse(
                resultModel.getId(),
                resultModel.getPostId(),
                resultModel.getParentCommentId(),
                resultModel.getContent(),
                resultModel.getDepth(),
                resultModel.getUpvoteCount(),
                resultModel.getDownvoteCount(),
                resultModel.isDeleted(),
                AuthorResponse.from(resultModel.getAuthor()),
                resultModel.getCreatedAt(),
                resultModel.getUpdatedAt(),
                childResponses
        );
    }
}
