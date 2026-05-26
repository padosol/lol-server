package com.example.lolserver.community.application;

import com.example.lolserver.community.application.command.CreatePostCommand;
import com.example.lolserver.community.application.command.PostSearchCommand;
import com.example.lolserver.community.application.command.UpdatePostCommand;
import com.example.lolserver.community.application.model.AuthorReadModel;
import com.example.lolserver.community.application.model.PostDetailReadModel;
import com.example.lolserver.community.application.model.PostListReadModel;
import com.example.lolserver.community.application.port.in.PostQueryUseCase;
import com.example.lolserver.community.application.port.in.PostUseCase;
import com.example.lolserver.community.application.port.out.PostPersistencePort;
import com.example.lolserver.community.application.port.out.VotePersistencePort;
import com.example.lolserver.community.domain.Post;
import com.example.lolserver.community.domain.Vote;
import com.example.lolserver.community.domain.vo.PostCategory;
import com.example.lolserver.community.domain.vo.VoteTargetType;
import com.example.lolserver.member.application.model.MemberProfileReadModel;
import com.example.lolserver.member.application.port.in.MemberQueryUseCase;
import com.example.lolserver.common.support.SliceResult;
import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService implements PostUseCase, PostQueryUseCase {

    private final PostPersistencePort postPersistencePort;
    private final MemberQueryUseCase memberQueryUseCase;
    private final VotePersistencePort votePersistencePort;

    @Override
    @Transactional
    public PostDetailReadModel createPost(Long memberId, CreatePostCommand command) {
        validateCategory(command.getCategory());

        MemberProfileReadModel author = memberQueryUseCase.getMemberProfile(memberId);

        Post post = Post.create(memberId, command.getTitle(),
                command.getContent(), command.getCategory());

        Post saved = postPersistencePort.save(post);

        return PostDetailReadModel.of(saved, author, null);
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

        MemberProfileReadModel author = memberQueryUseCase.getMemberProfile(memberId);

        return PostDetailReadModel.of(saved, author, null);
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

        MemberProfileReadModel author = memberQueryUseCase.getMemberProfile(post.getMemberId());

        Vote currentUserVote = null;
        if (currentMemberId != null) {
            currentUserVote = votePersistencePort
                    .findByMemberIdAndTargetTypeAndTargetId(currentMemberId, VoteTargetType.POST, postId)
                    .orElse(null);
        }

        return PostDetailReadModel.of(post, author, currentUserVote);
    }

    @Override
    public SliceResult<PostListReadModel> getPosts(PostSearchCommand command) {
        return enrichAuthors(postPersistencePort.findPosts(command));
    }

    @Override
    public SliceResult<PostListReadModel> searchPosts(PostSearchCommand command) {
        return enrichAuthors(postPersistencePort.searchPosts(command));
    }

    @Override
    public SliceResult<PostListReadModel> getMyPosts(Long memberId, int page) {
        return enrichAuthors(postPersistencePort.findByMemberId(memberId, page));
    }

    /**
     * 게시글 목록의 작성자 정보를 member port.in 으로 배치 보강한다.
     * (영속성 계층의 member 테이블 조인을 대체)
     */
    private SliceResult<PostListReadModel> enrichAuthors(SliceResult<PostListReadModel> slice) {
        List<Long> authorIds = slice.getContent().stream()
                .map(PostListReadModel::getAuthorId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, MemberProfileReadModel> profiles = memberQueryUseCase.getMemberProfiles(authorIds).stream()
                .collect(Collectors.toMap(MemberProfileReadModel::getId, Function.identity()));

        List<PostListReadModel> content = slice.getContent().stream()
                .map(rm -> rm.toBuilder()
                        .author(toAuthor(profiles.get(rm.getAuthorId())))
                        .build())
                .toList();

        return new SliceResult<>(content, slice.isHasNext());
    }

    private AuthorReadModel toAuthor(MemberProfileReadModel profile) {
        return profile != null ? AuthorReadModel.of(profile) : null;
    }

    private void validateCategory(String category) {
        try {
            PostCategory.valueOf(category);
        } catch (IllegalArgumentException e) {
            throw new CoreException(ErrorType.INVALID_CATEGORY);
        }
    }
}
