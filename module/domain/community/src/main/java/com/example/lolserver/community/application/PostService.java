package com.example.lolserver.community.application;

import com.example.lolserver.community.application.command.CreatePostCommand;
import com.example.lolserver.community.application.command.PostSearchCommand;
import com.example.lolserver.community.application.command.UpdatePostCommand;
import com.example.lolserver.community.application.model.readmodel.AuthorReadModel;
import com.example.lolserver.community.application.model.readmodel.PostDetailReadModel;
import com.example.lolserver.community.application.model.readmodel.PostListReadModel;
import com.example.lolserver.community.application.model.resultmodel.PostDetailResultModel;
import com.example.lolserver.community.application.port.in.CategoryQueryUseCase;
import com.example.lolserver.community.application.port.in.PostQueryUseCase;
import com.example.lolserver.community.application.port.in.PostUseCase;
import com.example.lolserver.community.application.port.out.BookmarkPersistencePort;
import com.example.lolserver.community.application.port.out.PostPersistencePort;
import com.example.lolserver.community.application.port.out.VotePersistencePort;
import com.example.lolserver.community.domain.Post;
import com.example.lolserver.community.domain.Vote;
import com.example.lolserver.community.domain.vo.VoteTargetType;
import com.example.lolserver.member.application.model.readmodel.MemberProfileReadModel;
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
    private final BookmarkPersistencePort bookmarkPersistencePort;
    private final VotePersistencePort votePersistencePort;
    private final CategoryQueryUseCase categoryQueryUseCase;

    @Override
    @Transactional
    public PostDetailResultModel createPost(Long memberId, CreatePostCommand command) {
        validateCategory(command.getCategory());

        MemberProfileReadModel author = memberQueryUseCase.getMemberProfile(memberId);

        Post post = Post.create(memberId, command.getTitle(),
                command.getContent(), command.getCategory());

        Post saved = postPersistencePort.save(post);

        // 방금 만든 글이므로 북마크되어 있을 수 없다.
        return PostDetailResultModel.of(saved, author, null, false);
    }

    @Override
    @Transactional
    public PostDetailResultModel updatePost(Long memberId, Long postId, UpdatePostCommand command) {
        validateCategory(command.getCategory());

        Post post = postPersistencePort.findById(postId)
                .orElseThrow(() -> new CoreException(ErrorType.POST_NOT_FOUND));

        post.validateOwner(memberId);

        post.updateContent(command.getTitle(), command.getContent(), command.getCategory());
        Post saved = postPersistencePort.save(post);

        MemberProfileReadModel author = memberQueryUseCase.getMemberProfile(memberId);

        // false 로 고정하면 자기 글을 북마크한 뒤 수정했을 때 응답이 해제 상태로
        // 돌아와 클라이언트 캐시가 뒤집힌다. 실제 상태를 조회한다.
        boolean bookmarked = bookmarkPersistencePort
                .existsByMemberIdAndPostId(memberId, postId);

        return PostDetailResultModel.of(saved, author, null, bookmarked);
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

        // 비로그인은 조회 없이 false. null 이 아니라 false 여야 응답에서 모호해지지 않는다.
        boolean currentUserBookmarked = currentMemberId != null
                && bookmarkPersistencePort
                        .existsByMemberIdAndPostId(currentMemberId, postId);

        return PostDetailReadModel.of(post, author, currentUserVote, currentUserBookmarked);
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

        // 빈 목록에 회원 조회를 날리지 않는다.
        if (authorIds.isEmpty()) {
            return slice;
        }

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

    /**
     * 카테고리 목록이 DB 로 옮겨가면서 enum 검증을 대체했다. enum 을 남겨두면
     * 게시판을 추가할 때마다 서버를 다시 배포해야 해서 DB 화의 의미가 없다.
     *
     * <p>존재 여부뿐 아니라 숨김/읽기 전용까지 함께 걸러진다 — enum 시절에는
     * 표현할 수 없던 검증이다.
     */
    private void validateCategory(String category) {
        categoryQueryUseCase.validateWritable(category);
    }
}
