package com.example.lolserver.domain.community.application.port.in;

import com.example.lolserver.domain.community.application.command.PostSearchCommand;
import com.example.lolserver.domain.community.application.model.PostDetailReadModel;
import com.example.lolserver.domain.community.application.model.PostListReadModel;
import com.example.lolserver.support.SliceResult;

public interface PostQueryUseCase {

    PostDetailReadModel getPost(Long postId, Long currentMemberId);

    SliceResult<PostListReadModel> getPosts(PostSearchCommand command);

    SliceResult<PostListReadModel> searchPosts(PostSearchCommand command);

    SliceResult<PostListReadModel> getMyPosts(Long memberId, int page);
}
