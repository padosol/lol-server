package com.example.lolserver.domain.community.application.port.out;

import com.example.lolserver.domain.community.application.command.PostSearchCommand;
import com.example.lolserver.domain.community.application.model.PostListReadModel;
import com.example.lolserver.domain.community.domain.Post;
import com.example.lolserver.support.SliceResult;

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
