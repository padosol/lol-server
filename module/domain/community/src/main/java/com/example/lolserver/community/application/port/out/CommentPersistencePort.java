package com.example.lolserver.community.application.port.out;

import com.example.lolserver.community.domain.Comment;

import java.util.List;
import java.util.Optional;

public interface CommentPersistencePort {

    Comment save(Comment comment);

    Optional<Comment> findById(Long id);

    List<Comment> findByPostId(Long postId);

    int countByPostId(Long postId);

    void updateVoteCounts(Long commentId, int upvoteCount, int downvoteCount);
}
