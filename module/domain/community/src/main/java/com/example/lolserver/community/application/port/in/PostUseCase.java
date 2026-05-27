package com.example.lolserver.community.application.port.in;

import com.example.lolserver.community.application.command.CreatePostCommand;
import com.example.lolserver.community.application.command.UpdatePostCommand;
import com.example.lolserver.community.application.model.resultmodel.PostDetailResultModel;

public interface PostUseCase {

    PostDetailResultModel createPost(Long memberId, CreatePostCommand command);

    PostDetailResultModel updatePost(Long memberId, Long postId, UpdatePostCommand command);

    void deletePost(Long memberId, Long postId);
}
