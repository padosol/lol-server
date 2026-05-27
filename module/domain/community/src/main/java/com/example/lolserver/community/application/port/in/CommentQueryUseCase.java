package com.example.lolserver.community.application.port.in;

import com.example.lolserver.community.application.model.readmodel.CommentTreeReadModel;

import java.util.List;

public interface CommentQueryUseCase {

    List<CommentTreeReadModel> getComments(Long postId);
}
