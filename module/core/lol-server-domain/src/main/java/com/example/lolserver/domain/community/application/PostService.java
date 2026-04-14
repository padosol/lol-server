package com.example.lolserver.domain.community.application;

import com.example.lolserver.domain.community.application.command.CreatePostCommand;
import com.example.lolserver.domain.community.application.command.PostSearchCommand;
import com.example.lolserver.domain.community.application.command.UpdatePostCommand;
import com.example.lolserver.domain.community.application.model.PostDetailReadModel;
import com.example.lolserver.domain.community.application.model.PostListReadModel;
import com.example.lolserver.domain.community.application.port.in.PostQueryUseCase;
import com.example.lolserver.domain.community.application.port.in.PostUseCase;
import com.example.lolserver.domain.community.application.port.out.PostPersistencePort;
import com.example.lolserver.domain.community.application.port.out.VotePersistencePort;
import com.example.lolserver.domain.community.domain.Post;
import com.example.lolserver.domain.community.domain.Vote;
import com.example.lolserver.domain.community.domain.vo.PostCategory;
import com.example.lolserver.domain.community.domain.vo.VoteTargetType;
import com.example.lolserver.domain.member.application.port.out.MemberPersistencePort;
import com.example.lolserver.domain.member.domain.Member;
import com.example.lolserver.support.SliceResult;
import com.example.lolserver.support.error.CoreException;
import com.example.lolserver.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService implements PostUseCase, PostQueryUseCase {

    private final PostPersistencePort postPersistencePort;
    private final MemberPersistencePort memberPersistencePort;
    private final VotePersistencePort votePersistencePort;

    @Override
    @Transactional
    public PostDetailReadModel createPost(Long memberId, CreatePostCommand command) {
        validateCategory(command.getCategory());

        Member member = memberPersistencePort.findById(memberId)
                .orElseThrow(() -> new CoreException(ErrorType.MEMBER_NOT_FOUND));

        Post post = Post.create(memberId, command.getTitle(),
                command.getContent(), command.getCategory());

        Post saved = postPersistencePort.save(post);

        return PostDetailReadModel.of(saved, member, null);
    }

    @Override
    @Transactional
    public PostDetailReadModel updatePost(Long memberId, Long postId, UpdatePostCommand command) {
        validateCategory(command.getCategory());

        Post post = postPersistencePort.findById(postId)
                .orElseThrow(() -> new CoreException(ErrorType.POST_NOT_FOUND));

        post.validateOwner(memberId);

        post.updateContent(command.getTitle(), command.getContent(), command.getCategory());
        Post saved = postPersistencePort.save(post);

        Member member = memberPersistencePort.findById(memberId)
                .orElseThrow(() -> new CoreException(ErrorType.MEMBER_NOT_FOUND));

        return PostDetailReadModel.of(saved, member, null);
    }

    @Override
    @Transactional
    public void deletePost(Long memberId, Long postId) {
        Post post = postPersistencePort.findById(postId)
                .orElseThrow(() -> new CoreException(ErrorType.POST_NOT_FOUND));

        post.validateOwner(memberId);

        post.markDeleted();
        postPersistencePort.save(post);
    }

    @Override
    @Transactional
    public PostDetailReadModel getPost(Long postId, Long currentMemberId) {
        Post post = postPersistencePort.findById(postId)
                .orElseThrow(() -> new CoreException(ErrorType.POST_NOT_FOUND));

        post.validateNotDeleted();

        postPersistencePort.incrementViewCount(postId);
        post.incrementViewCount();

        Member member = memberPersistencePort.findById(post.getMemberId())
                .orElseThrow(() -> new CoreException(ErrorType.MEMBER_NOT_FOUND));

        Vote currentUserVote = null;
        if (currentMemberId != null) {
            currentUserVote = votePersistencePort
                    .findByMemberIdAndTargetTypeAndTargetId(currentMemberId, VoteTargetType.POST, postId)
                    .orElse(null);
        }

        return PostDetailReadModel.of(post, member, currentUserVote);
    }

    @Override
    public SliceResult<PostListReadModel> getPosts(PostSearchCommand command) {
        return postPersistencePort.findPosts(command);
    }

    @Override
    public SliceResult<PostListReadModel> searchPosts(PostSearchCommand command) {
        return postPersistencePort.searchPosts(command);
    }

    @Override
    public SliceResult<PostListReadModel> getMyPosts(Long memberId, int page) {
        return postPersistencePort.findByMemberId(memberId, page);
    }

    private void validateCategory(String category) {
        try {
            PostCategory.valueOf(category);
        } catch (IllegalArgumentException e) {
            throw new CoreException(ErrorType.INVALID_CATEGORY);
        }
    }
}
