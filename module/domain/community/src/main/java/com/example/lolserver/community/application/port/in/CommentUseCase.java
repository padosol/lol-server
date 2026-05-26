package com.example.lolserver.community.application.port.in;

import com.example.lolserver.community.application.command.CreateCommentCommand;
import com.example.lolserver.community.application.command.UpdateCommentCommand;
import com.example.lolserver.community.application.model.CommentTreeReadModel;

public interface CommentUseCase {

    CommentTreeReadModel createComment(Long memberId, Long postId, CreateCommentCommand command);

    CommentTreeReadModel updateComment(Long memberId, Long commentId, UpdateCommentCommand command);

    void deleteComment(Long memberId, Long commentId);
}
