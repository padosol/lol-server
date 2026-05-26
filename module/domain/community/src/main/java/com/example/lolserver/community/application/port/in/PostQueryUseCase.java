package com.example.lolserver.community.application.port.in;

import com.example.lolserver.community.application.command.PostSearchCommand;
import com.example.lolserver.community.application.model.PostDetailReadModel;
import com.example.lolserver.community.application.model.PostListReadModel;
import com.example.lolserver.common.support.SliceResult;

public interface PostQueryUseCase {

    PostDetailReadModel getPost(Long postId, Long currentMemberId);

    SliceResult<PostListReadModel> getPosts(PostSearchCommand command);

    SliceResult<PostListReadModel> searchPosts(PostSearchCommand command);

    SliceResult<PostListReadModel> getMyPosts(Long memberId, int page);
}
