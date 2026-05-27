package com.example.lolserver.community.application;

import com.example.lolserver.community.application.command.CreateCommentCommand;
import com.example.lolserver.community.application.command.UpdateCommentCommand;
import com.example.lolserver.community.application.model.readmodel.CommentTreeReadModel;
import com.example.lolserver.community.application.model.resultmodel.CommentTreeResultModel;
import com.example.lolserver.community.application.port.in.CommentQueryUseCase;
import com.example.lolserver.community.application.port.in.CommentUseCase;
import com.example.lolserver.community.application.port.out.CommentPersistencePort;
import com.example.lolserver.community.application.port.out.PostPersistencePort;
import com.example.lolserver.community.domain.Comment;
import com.example.lolserver.member.application.model.readmodel.MemberProfileReadModel;
import com.example.lolserver.member.application.port.in.MemberQueryUseCase;
import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService implements CommentUseCase, CommentQueryUseCase {

    private static final int MAX_DEPTH = 5;
    private final CommentPersistencePort commentPersistencePort;
    private final PostPersistencePort postPersistencePort;
    private final MemberQueryUseCase memberQueryUseCase;

    @Override
    @Transactional
    public CommentTreeResultModel createComment(Long memberId, Long postId, CreateCommentCommand command) {
        postPersistencePort.findById(postId)
                .orElseThrow(() -> new CoreException(ErrorType.POST_NOT_FOUND));

        MemberProfileReadModel author = memberQueryUseCase.getMemberProfile(memberId);

        int depth = 0;
        if (command.getParentCommentId() != null) {
            Comment parent = commentPersistencePort.findById(command.getParentCommentId())
                    .orElseThrow(() -> new CoreException(ErrorType.COMMENT_NOT_FOUND));
            depth = parent.getDepth() + 1;
            if (depth > MAX_DEPTH) {
                throw new CoreException(ErrorType.COMMENT_DEPTH_EXCEEDED);
            }
        }

        Comment comment = Comment.create(postId, memberId, command.getContent(),
                command.getParentCommentId(), depth);

        Comment saved = commentPersistencePort.save(comment);

        int commentCount = commentPersistencePort.countByPostId(postId);
        postPersistencePort.updateCommentCount(postId, commentCount);

        return CommentTreeResultModel.of(saved, author);
    }

    @Override
    @Transactional
    public CommentTreeResultModel updateComment(Long memberId, Long commentId, UpdateCommentCommand command) {
        Comment comment = commentPersistencePort.findById(commentId)
                .orElseThrow(() -> new CoreException(ErrorType.COMMENT_NOT_FOUND));

        comment.validateOwner(memberId);

        comment.updateContent(command.getContent());
        Comment saved = commentPersistencePort.save(comment);

        MemberProfileReadModel author = memberQueryUseCase.getMemberProfile(memberId);

        return CommentTreeResultModel.of(saved, author);
    }

    @Override
    @Transactional
    public void deleteComment(Long memberId, Long commentId) {
        Comment comment = commentPersistencePort.findById(commentId)
                .orElseThrow(() -> new CoreException(ErrorType.COMMENT_NOT_FOUND));

        comment.validateOwner(memberId);

        comment.markDeleted();
        commentPersistencePort.save(comment);

        int commentCount = commentPersistencePort.countByPostId(comment.getPostId());
        postPersistencePort.updateCommentCount(comment.getPostId(), commentCount);
    }

    @Override
    public List<CommentTreeReadModel> getComments(Long postId) {
        postPersistencePort.findById(postId)
                .orElseThrow(() -> new CoreException(ErrorType.POST_NOT_FOUND));

        List<Comment> allComments = commentPersistencePort.findByPostId(postId);

        List<Long> memberIds = allComments.stream()
                .map(Comment::getMemberId)
                .distinct()
                .toList();
        Map<Long, MemberProfileReadModel> profileMap = memberQueryUseCase.getMemberProfiles(memberIds).stream()
                .collect(Collectors.toMap(MemberProfileReadModel::getId, Function.identity()));

        Map<Long, CommentTreeReadModel> nodeMap = new HashMap<>();
        for (Comment comment : allComments) {
            MemberProfileReadModel author = profileMap.get(comment.getMemberId());
            nodeMap.put(comment.getId(), CommentTreeReadModel.of(comment, author));
        }

        List<CommentTreeReadModel> rootNodes = new ArrayList<>();
        for (Comment comment : allComments) {
            CommentTreeReadModel node = nodeMap.get(comment.getId());
            if (comment.getParentCommentId() == null) {
                rootNodes.add(node);
            } else {
                CommentTreeReadModel parent =
                        nodeMap.get(comment.getParentCommentId());
                if (parent != null) {
                    parent.getChildren().add(node);
                }
            }
        }

        return rootNodes;
    }
}
