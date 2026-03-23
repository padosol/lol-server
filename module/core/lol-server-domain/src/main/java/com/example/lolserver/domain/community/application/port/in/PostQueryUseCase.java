package com.example.lolserver.domain.community.application.port.in;

import com.example.lolserver.domain.community.application.command.PostSearchCommand;
import com.example.lolserver.domain.community.application.model.PostDetailReadModel;
import com.example.lolserver.domain.community.application.model.PostListReadModel;
import com.example.lolserver.support.Page;

public interface PostQueryUseCase {

    PostDetailReadModel getPost(Long postId, Long currentMemberId);

    Page<PostListReadModel> getPosts(PostSearchCommand command);

    Page<PostListReadModel> searchPosts(PostSearchCommand command);

    Page<PostListReadModel> getMyPosts(Long memberId, int page);
}
