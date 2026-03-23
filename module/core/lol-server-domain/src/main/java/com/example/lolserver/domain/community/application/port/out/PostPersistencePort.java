package com.example.lolserver.domain.community.application.port.out;

import com.example.lolserver.domain.community.application.command.PostSearchCommand;
import com.example.lolserver.domain.community.application.model.PostListReadModel;
import com.example.lolserver.domain.community.domain.Post;
import com.example.lolserver.support.Page;

import java.util.Optional;

public interface PostPersistencePort {

    Post save(Post post);

    Optional<Post> findById(Long id);

    Page<PostListReadModel> findPosts(PostSearchCommand command);

    Page<PostListReadModel> searchPosts(PostSearchCommand command);

    Page<PostListReadModel> findByMemberId(Long memberId, int page);

    void incrementViewCount(Long postId);

    void updateVoteCounts(Long postId, int upvoteCount, int downvoteCount);

    void updateCommentCount(Long postId, int commentCount);

    void updateHotScore(Long postId, double hotScore);
}
