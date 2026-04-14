package com.example.lolserver.domain.community.application;

import com.example.lolserver.domain.community.application.command.CreateCommentCommand;
import com.example.lolserver.domain.community.application.command.UpdateCommentCommand;
import com.example.lolserver.domain.community.application.model.CommentTreeReadModel;
import com.example.lolserver.domain.community.application.port.in.CommentQueryUseCase;
import com.example.lolserver.domain.community.application.port.in.CommentUseCase;
import com.example.lolserver.domain.community.application.port.out.CommentPersistencePort;
import com.example.lolserver.domain.community.application.port.out.PostPersistencePort;
import com.example.lolserver.domain.community.domain.Comment;
import com.example.lolserver.domain.member.application.port.out.MemberPersistencePort;
import com.example.lolserver.domain.member.domain.Member;
import com.example.lolserver.support.error.CoreException;
import com.example.lolserver.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService implements CommentUseCase, CommentQueryUseCase {

    private static final int MAX_DEPTH = 5;
    private final CommentPersistencePort commentPersistencePort;
    private final PostPersistencePort postPersistencePort;
    private final MemberPersistencePort memberPersistencePort;

    @Override
    @Transactional
    public CommentTreeReadModel createComment(Long memberId, Long postId, CreateCommentCommand command) {
        postPersistencePort.findById(postId)
                .orElseThrow(() -> new CoreException(ErrorType.POST_NOT_FOUND));

        Member member = memberPersistencePort.findById(memberId)
                .orElseThrow(() -> new CoreException(ErrorType.MEMBER_NOT_FOUND));

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

        return CommentTreeReadModel.of(saved, member);
    }

    @Override
    @Transactional
    public CommentTreeReadModel updateComment(Long memberId, Long commentId, UpdateCommentCommand command) {
        Comment comment = commentPersistencePort.findById(commentId)
                .orElseThrow(() -> new CoreException(ErrorType.COMMENT_NOT_FOUND));

        comment.validateOwner(memberId);

        comment.updateContent(command.getContent());
        Comment saved = commentPersistencePort.save(comment);

        Member member = memberPersistencePort.findById(memberId)
                .orElseThrow(() -> new CoreException(ErrorType.MEMBER_NOT_FOUND));

        return CommentTreeReadModel.of(saved, member);
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

        Map<Long, Member> memberCache = new HashMap<>();
        for (Comment comment : allComments) {
            memberCache.computeIfAbsent(comment.getMemberId(),
                    id -> memberPersistencePort.findById(id).orElse(null));
        }

        Map<Long, CommentTreeReadModel> nodeMap = new HashMap<>();
        for (Comment comment : allComments) {
            Member member = memberCache.get(comment.getMemberId());
            nodeMap.put(comment.getId(), CommentTreeReadModel.of(comment, member));
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
