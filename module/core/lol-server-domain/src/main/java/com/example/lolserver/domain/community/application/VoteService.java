package com.example.lolserver.domain.community.application;

import com.example.lolserver.domain.community.application.command.VoteCommand;
import com.example.lolserver.domain.community.application.model.VoteReadModel;
import com.example.lolserver.domain.community.application.port.in.VoteUseCase;
import com.example.lolserver.domain.community.application.port.out.CommentPersistencePort;
import com.example.lolserver.domain.community.application.port.out.PostPersistencePort;
import com.example.lolserver.domain.community.application.port.out.VotePersistencePort;
import com.example.lolserver.domain.community.domain.Vote;
import com.example.lolserver.domain.community.domain.vo.VoteTargetType;
import com.example.lolserver.domain.community.domain.vo.VoteType;
import com.example.lolserver.support.error.CoreException;
import com.example.lolserver.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VoteService implements VoteUseCase {

    private final VotePersistencePort votePersistencePort;
    private final PostPersistencePort postPersistencePort;
    private final CommentPersistencePort commentPersistencePort;

    @Override
    @Transactional
    public VoteReadModel vote(Long memberId, VoteCommand command) {
        validateTarget(command.getTargetType(), command.getTargetId());

        Optional<Vote> existingVote = votePersistencePort
                .findByMemberIdAndTargetTypeAndTargetId(
                        memberId, command.getTargetType(),
                        command.getTargetId());

        if (existingVote.isPresent()) {
            Vote vote = existingVote.get();
            if (vote.getVoteType() == command.getVoteType()) {
                return currentCounts(
                        command.getTargetType(),
                        command.getTargetId(),
                        command.getVoteType());
            }
            vote.changeVoteType(command.getVoteType());
            votePersistencePort.save(vote);
        } else {
            Vote vote = Vote.create(memberId, command.getTargetType(),
                    command.getTargetId(), command.getVoteType());
            votePersistencePort.save(vote);
        }

        return recalculateCounts(
                command.getTargetType(),
                command.getTargetId(),
                command.getVoteType());
    }

    @Override
    @Transactional
    public void removeVote(
            Long memberId, VoteTargetType targetType, Long targetId) {
        Vote vote = votePersistencePort
                .findByMemberIdAndTargetTypeAndTargetId(
                        memberId, targetType, targetId)
                .orElseThrow(() -> new CoreException(
                        ErrorType.VOTE_TARGET_NOT_FOUND));

        votePersistencePort.delete(vote);

        recalculateCounts(targetType, targetId, null);
    }

    private void validateTarget(
            VoteTargetType targetType, Long targetId) {
        if (targetType == VoteTargetType.POST) {
            postPersistencePort.findById(targetId)
                    .orElseThrow(() -> new CoreException(
                            ErrorType.VOTE_TARGET_NOT_FOUND));
        } else {
            commentPersistencePort.findById(targetId)
                    .orElseThrow(() -> new CoreException(
                            ErrorType.VOTE_TARGET_NOT_FOUND));
        }
    }

    private VoteReadModel recalculateCounts(
            VoteTargetType targetType, Long targetId,
            VoteType voteType) {
        int upvoteCount = votePersistencePort
                .countByTargetTypeAndTargetIdAndVoteType(
                        targetType, targetId, VoteType.UPVOTE);
        int downvoteCount = votePersistencePort
                .countByTargetTypeAndTargetIdAndVoteType(
                        targetType, targetId, VoteType.DOWNVOTE);

        if (targetType == VoteTargetType.POST) {
            postPersistencePort.updateVoteCounts(
                    targetId, upvoteCount, downvoteCount);
            updateHotScore(targetId, upvoteCount, downvoteCount);
        } else {
            commentPersistencePort.updateVoteCounts(
                    targetId, upvoteCount, downvoteCount);
        }

        return new VoteReadModel(
                targetType, targetId, voteType,
                upvoteCount, downvoteCount);
    }

    private VoteReadModel currentCounts(
            VoteTargetType targetType, Long targetId,
            VoteType voteType) {
        int upvoteCount = votePersistencePort
                .countByTargetTypeAndTargetIdAndVoteType(
                        targetType, targetId, VoteType.UPVOTE);
        int downvoteCount = votePersistencePort
                .countByTargetTypeAndTargetIdAndVoteType(
                        targetType, targetId, VoteType.DOWNVOTE);
        return new VoteReadModel(
                targetType, targetId, voteType,
                upvoteCount, downvoteCount);
    }

    private void updateHotScore(
            Long postId, int upvoteCount, int downvoteCount) {
        postPersistencePort.findById(postId).ifPresent(post -> {
            post.applyVoteCounts(upvoteCount, downvoteCount);
            postPersistencePort.updateHotScore(postId, post.getHotScore());
        });
    }
}
