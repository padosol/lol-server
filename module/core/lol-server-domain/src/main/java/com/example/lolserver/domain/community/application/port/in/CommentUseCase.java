package com.example.lolserver.domain.community.application.port.in;

import com.example.lolserver.domain.community.application.command.CreateCommentCommand;
import com.example.lolserver.domain.community.application.command.UpdateCommentCommand;
import com.example.lolserver.domain.community.application.model.CommentTreeReadModel;

public interface CommentUseCase {

    CommentTreeReadModel createComment(Long memberId, Long postId, CreateCommentCommand command);

    CommentTreeReadModel updateComment(Long memberId, Long commentId, UpdateCommentCommand command);

    void deleteComment(Long memberId, Long commentId);
}
