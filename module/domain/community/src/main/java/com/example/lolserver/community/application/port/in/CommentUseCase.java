package com.example.lolserver.community.application.port.in;

import com.example.lolserver.community.application.command.CreateCommentCommand;
import com.example.lolserver.community.application.command.UpdateCommentCommand;
import com.example.lolserver.community.application.model.resultmodel.CommentTreeResultModel;

public interface CommentUseCase {

    CommentTreeResultModel createComment(Long memberId, Long postId, CreateCommentCommand command);

    CommentTreeResultModel updateComment(Long memberId, Long commentId, UpdateCommentCommand command);

    void deleteComment(Long memberId, Long commentId);
}
