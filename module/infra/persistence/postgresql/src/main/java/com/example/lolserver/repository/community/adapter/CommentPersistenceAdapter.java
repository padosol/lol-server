package com.example.lolserver.repository.community.adapter;

import com.example.lolserver.domain.community.application.port.out.CommentPersistencePort;
import com.example.lolserver.domain.community.domain.Comment;
import com.example.lolserver.repository.community.entity.CommunityCommentEntity;
import com.example.lolserver.repository.community.mapper.CommunityCommentMapper;
import com.example.lolserver.repository.community.repository.CommunityCommentJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CommentPersistenceAdapter implements CommentPersistencePort {

    private final CommunityCommentJpaRepository commentJpaRepository;
    private final CommunityCommentMapper commentMapper;

    @Override
    public Comment save(Comment comment) {
        CommunityCommentEntity entity = commentMapper.toEntity(comment);
        CommunityCommentEntity saved = commentJpaRepository.save(entity);
        return commentMapper.toDomain(saved);
    }

    @Override
    public Optional<Comment> findById(Long id) {
        return commentJpaRepository.findById(id)
                .map(commentMapper::toDomain);
    }

    @Override
    public List<Comment> findByPostId(Long postId) {
        return commentMapper.toDomainList(
                commentJpaRepository.findByPostIdOrderByCreatedAtAsc(postId));
    }

    @Override
    public int countByPostId(Long postId) {
        return commentJpaRepository.countByPostIdAndDeletedFalse(postId);
    }

    @Override
    public void updateVoteCounts(
            Long commentId, int upvoteCount, int downvoteCount) {
        commentJpaRepository.updateVoteCounts(
                commentId, upvoteCount, downvoteCount);
    }
}
