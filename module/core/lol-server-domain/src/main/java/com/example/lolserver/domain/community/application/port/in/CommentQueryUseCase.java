package com.example.lolserver.domain.community.application.port.in;

import com.example.lolserver.domain.community.application.model.CommentTreeReadModel;

import java.util.List;

public interface CommentQueryUseCase {

    List<CommentTreeReadModel> getComments(Long postId);
}
