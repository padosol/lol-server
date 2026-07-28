package com.example.lolserver.community.application.port.out;

import com.example.lolserver.community.application.command.PostSearchCommand;
import com.example.lolserver.community.application.model.readmodel.PostListReadModel;
import com.example.lolserver.community.domain.Post;
import com.example.lolserver.common.support.SliceResult;

import java.util.Optional;

public interface PostPersistencePort {

    Post save(Post post);

    Optional<Post> findById(Long id);

    SliceResult<PostListReadModel> findPosts(PostSearchCommand command);

    SliceResult<PostListReadModel> searchPosts(PostSearchCommand command);

    SliceResult<PostListReadModel> findByMemberId(Long memberId, int page);

    void incrementViewCount(Long postId);

    void updateVoteCounts(Long postId, int upvoteCount, int downvoteCount);

    void updateCommentCount(Long postId, int commentCount);

    void updateHotScore(Long postId, double hotScore);
}
