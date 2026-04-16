package com.example.lolserver.domain.community.application.port.in;

import com.example.lolserver.domain.community.application.command.CreatePostCommand;
import com.example.lolserver.domain.community.application.command.UpdatePostCommand;
import com.example.lolserver.domain.community.application.model.PostDetailReadModel;

public interface PostUseCase {

    PostDetailReadModel createPost(Long memberId, CreatePostCommand command);

    PostDetailReadModel updatePost(Long memberId, Long postId, UpdatePostCommand command);

    void deletePost(Long memberId, Long postId);
}
