package com.example.lolserver.domain.community.application;

import com.example.lolserver.domain.community.application.command.CreatePostCommand;
import com.example.lolserver.domain.community.application.command.PostSearchCommand;
import com.example.lolserver.domain.community.application.command.UpdatePostCommand;
import com.example.lolserver.domain.community.application.model.AuthorReadModel;
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
import com.example.lolserver.support.Page;
import com.example.lolserver.support.error.CoreException;
import com.example.lolserver.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
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

        Post post = new Post();
        post.setMemberId(memberId);
        post.setTitle(command.getTitle());
        post.setContent(command.getContent());
        post.setCategory(command.getCategory());
        post.setCreatedAt(LocalDateTime.now());
        post.setUpdatedAt(LocalDateTime.now());
        post.calculateHotScore();

        Post saved = postPersistencePort.save(post);

        return toDetailReadModel(saved, member, null);
    }

    @Override
    @Transactional
    public PostDetailReadModel updatePost(Long memberId, Long postId, UpdatePostCommand command) {
        validateCategory(command.getCategory());

        Post post = postPersistencePort.findById(postId)
                .orElseThrow(() -> new CoreException(ErrorType.POST_NOT_FOUND));

        if (!post.isOwner(memberId)) {
            throw new CoreException(ErrorType.FORBIDDEN);
        }

        post.updateContent(command.getTitle(), command.getContent(), command.getCategory());
        Post saved = postPersistencePort.save(post);

        Member member = memberPersistencePort.findById(memberId)
                .orElseThrow(() -> new CoreException(ErrorType.MEMBER_NOT_FOUND));

        return toDetailReadModel(saved, member, null);
    }

    @Override
    @Transactional
    public void deletePost(Long memberId, Long postId) {
        Post post = postPersistencePort.findById(postId)
                .orElseThrow(() -> new CoreException(ErrorType.POST_NOT_FOUND));

        if (!post.isOwner(memberId)) {
            throw new CoreException(ErrorType.FORBIDDEN);
        }

        post.markDeleted();
        postPersistencePort.save(post);
    }

    @Override
    @Transactional
    public PostDetailReadModel getPost(Long postId, Long currentMemberId) {
        Post post = postPersistencePort.findById(postId)
                .orElseThrow(() -> new CoreException(ErrorType.POST_NOT_FOUND));

        if (post.isDeleted()) {
            throw new CoreException(ErrorType.POST_NOT_FOUND);
        }

        postPersistencePort.incrementViewCount(postId);
        post.setViewCount(post.getViewCount() + 1);

        Member member = memberPersistencePort.findById(post.getMemberId())
                .orElseThrow(() -> new CoreException(ErrorType.MEMBER_NOT_FOUND));

        Vote currentUserVote = null;
        if (currentMemberId != null) {
            currentUserVote = votePersistencePort
                    .findByMemberIdAndTargetTypeAndTargetId(currentMemberId, VoteTargetType.POST, postId)
                    .orElse(null);
        }

        return toDetailReadModel(post, member, currentUserVote);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PostListReadModel> getPosts(PostSearchCommand command) {
        return postPersistencePort.findPosts(command);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PostListReadModel> searchPosts(PostSearchCommand command) {
        return postPersistencePort.searchPosts(command);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PostListReadModel> getMyPosts(Long memberId, int page) {
        return postPersistencePort.findByMemberId(memberId, page);
    }

    private void validateCategory(String category) {
        try {
            PostCategory.valueOf(category);
        } catch (IllegalArgumentException e) {
            throw new CoreException(ErrorType.INVALID_CATEGORY);
        }
    }

    private PostDetailReadModel toDetailReadModel(
            Post post, Member member, Vote currentUserVote) {
        return PostDetailReadModel.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .category(post.getCategory())
                .viewCount(post.getViewCount())
                .upvoteCount(post.getUpvoteCount())
                .downvoteCount(post.getDownvoteCount())
                .commentCount(post.getCommentCount())
                .author(AuthorReadModel.of(member))
                .currentUserVote(
                        currentUserVote != null
                                ? currentUserVote.getVoteType() : null)
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }
}
