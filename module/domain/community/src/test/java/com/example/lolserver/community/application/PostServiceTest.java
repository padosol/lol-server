package com.example.lolserver.community.application;

import com.example.lolserver.community.application.command.CreatePostCommand;
import com.example.lolserver.community.application.command.PostSearchCommand;
import com.example.lolserver.community.application.command.UpdatePostCommand;
import com.example.lolserver.community.application.model.readmodel.PostDetailReadModel;
import com.example.lolserver.community.application.model.readmodel.PostListReadModel;
import com.example.lolserver.community.application.model.resultmodel.PostDetailResultModel;
import com.example.lolserver.community.application.port.out.BookmarkPersistencePort;
import com.example.lolserver.community.application.port.out.PostPersistencePort;
import com.example.lolserver.community.application.port.out.VotePersistencePort;
import com.example.lolserver.community.domain.Post;
import com.example.lolserver.community.domain.vo.SortType;
import com.example.lolserver.member.application.model.readmodel.MemberProfileReadModel;
import com.example.lolserver.member.application.port.in.MemberQueryUseCase;
import com.example.lolserver.common.support.SliceResult;
import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostPersistencePort postPersistencePort;

    @Mock
    private MemberQueryUseCase memberQueryUseCase;

    @Mock
    private BookmarkPersistencePort bookmarkPersistencePort;

    @Mock
    private VotePersistencePort votePersistencePort;

    @InjectMocks
    private PostService postService;

    @DisplayName("게시글을 작성하면 생성된 게시글 정보를 반환한다")
    @Test
    void createPost_success() {
        // given
        Long memberId = 1L;
        CreatePostCommand command = CreatePostCommand.builder()
                .title("테스트 제목")
                .content("테스트 내용")
                .category("GENERAL")
                .build();

        Post savedPost = createPost(1L, memberId);

        given(memberQueryUseCase.getMemberProfile(memberId))
                .willReturn(createProfile(memberId));
        given(postPersistencePort.save(any(Post.class)))
                .willReturn(savedPost);

        // when
        PostDetailResultModel result = postService.createPost(memberId, command);

        // then
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("테스트 제목");
        assertThat(result.getAuthor().id()).isEqualTo(memberId);
    }

    @DisplayName("유효하지 않은 카테고리로 게시글 작성 시 예외가 발생한다")
    @Test
    void createPost_invalidCategory() {
        // given
        Long memberId = 1L;
        CreatePostCommand command = CreatePostCommand.builder()
                .title("제목")
                .content("내용")
                .category("INVALID")
                .build();

        // when & then
        assertThatThrownBy(() -> postService.createPost(memberId, command))
                .isInstanceOf(CoreException.class)
                .extracting(e -> ((CoreException) e).getErrorType())
                .isEqualTo(ErrorType.INVALID_CATEGORY);
    }

    @DisplayName("본인 게시글을 수정하면 수정된 정보를 반환한다")
    @Test
    void updatePost_success() {
        // given
        Long memberId = 1L;
        Long postId = 1L;
        UpdatePostCommand command = UpdatePostCommand.builder()
                .title("수정된 제목")
                .content("수정된 내용")
                .category("GENERAL")
                .build();

        Post post = createPost(postId, memberId);

        given(postPersistencePort.findById(postId))
                .willReturn(Optional.of(post));
        given(postPersistencePort.save(any(Post.class)))
                .willReturn(post);
        given(memberQueryUseCase.getMemberProfile(memberId))
                .willReturn(createProfile(memberId));

        // when
        PostDetailResultModel result =
                postService.updatePost(memberId, postId, command);

        // then
        assertThat(result).isNotNull();
    }

    @DisplayName("북마크한 자기 글을 수정해도 응답의 북마크 여부는 true 로 유지된다")
    @Test
    void updatePost_keepsBookmarkedState() {
        // given — false 로 고정하면 클라이언트 캐시가 해제 상태로 뒤집힌다
        Long memberId = 1L;
        Long postId = 1L;
        UpdatePostCommand command = UpdatePostCommand.builder()
                .title("수정된 제목").content("수정된 내용").category("GENERAL")
                .build();
        Post post = createPost(postId, memberId);

        given(postPersistencePort.findById(postId)).willReturn(Optional.of(post));
        given(postPersistencePort.save(any(Post.class))).willReturn(post);
        given(memberQueryUseCase.getMemberProfile(memberId))
                .willReturn(createProfile(memberId));
        given(bookmarkPersistencePort.existsByMemberIdAndPostId(memberId, postId))
                .willReturn(true);

        // when
        PostDetailResultModel result = postService.updatePost(memberId, postId, command);

        // then
        assertThat(result.isCurrentUserBookmarked()).isTrue();
    }

    @DisplayName("게시글을 새로 작성하면 북마크 여부는 항상 false 이고 조회하지 않는다")
    @Test
    void createPost_neverBookmarked() {
        // given
        Long memberId = 1L;
        CreatePostCommand command = CreatePostCommand.builder()
                .title("제목").content("내용").category("GENERAL")
                .build();
        given(memberQueryUseCase.getMemberProfile(memberId))
                .willReturn(createProfile(memberId));
        given(postPersistencePort.save(any(Post.class)))
                .willReturn(createPost(1L, memberId));

        // when
        PostDetailResultModel result = postService.createPost(memberId, command);

        // then — 방금 만든 글이므로 조회 없이 false 여야 한다
        assertThat(result.isCurrentUserBookmarked()).isFalse();
        then(bookmarkPersistencePort).shouldHaveNoInteractions();
    }

    @DisplayName("다른 사람의 게시글을 수정하면 FORBIDDEN 예외가 발생한다")
    @Test
    void updatePost_forbidden() {
        // given
        Long memberId = 1L;
        Long otherMemberId = 2L;
        Long postId = 1L;
        UpdatePostCommand command = UpdatePostCommand.builder()
                .title("수정")
                .content("내용")
                .category("GENERAL")
                .build();

        Post post = createPost(postId, otherMemberId);

        given(postPersistencePort.findById(postId))
                .willReturn(Optional.of(post));

        // when & then
        assertThatThrownBy(
                () -> postService.updatePost(memberId, postId, command))
                .isInstanceOf(CoreException.class)
                .extracting(e -> ((CoreException) e).getErrorType())
                .isEqualTo(ErrorType.FORBIDDEN);
    }

    @DisplayName("본인 게시글을 삭제하면 soft delete 처리된다")
    @Test
    void deletePost_success() {
        // given
        Long memberId = 1L;
        Long postId = 1L;
        Post post = createPost(postId, memberId);

        given(postPersistencePort.findById(postId))
                .willReturn(Optional.of(post));
        given(postPersistencePort.save(any(Post.class)))
                .willReturn(post);

        // when
        postService.deletePost(memberId, postId);

        // then
        then(postPersistencePort).should().save(any(Post.class));
    }

    @DisplayName("존재하지 않는 게시글 삭제 시 예외가 발생한다")
    @Test
    void deletePost_notFound() {
        // given
        given(postPersistencePort.findById(999L))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> postService.deletePost(1L, 999L))
                .isInstanceOf(CoreException.class)
                .extracting(e -> ((CoreException) e).getErrorType())
                .isEqualTo(ErrorType.POST_NOT_FOUND);
    }

    @DisplayName("게시글 상세 조회 시 조회수가 증가하고 상세 정보를 반환한다")
    @Test
    void getPost_success() {
        // given
        Long postId = 1L;
        Long memberId = 1L;
        Post post = createPost(postId, memberId);

        given(postPersistencePort.findById(postId))
                .willReturn(Optional.of(post));
        given(memberQueryUseCase.getMemberProfile(memberId))
                .willReturn(createProfile(memberId));

        // when
        PostDetailReadModel result = postService.getPost(postId, null);

        // then
        assertThat(result.getId()).isEqualTo(postId);
        then(postPersistencePort).should().incrementViewCount(postId);
    }

    @DisplayName("비로그인 상세 조회 시 북마크 여부는 false 이고 북마크를 조회하지 않는다")
    @Test
    void getPost_anonymous_bookmarkFalse() {
        // given
        Long postId = 1L;
        Long authorId = 1L;
        given(postPersistencePort.findById(postId))
                .willReturn(Optional.of(createPost(postId, authorId)));
        given(memberQueryUseCase.getMemberProfile(authorId))
                .willReturn(createProfile(authorId));

        // when
        PostDetailReadModel result = postService.getPost(postId, null);

        // then — null 이 아니라 false 여야 한다 (응답에서 구분 불가한 값이 되면 안 된다)
        assertThat(result.isCurrentUserBookmarked()).isFalse();
        then(bookmarkPersistencePort).shouldHaveNoInteractions();
    }

    @DisplayName("북마크한 게시글을 상세 조회하면 북마크 여부가 true 이다")
    @Test
    void getPost_bookmarked_returnsTrue() {
        // given
        Long postId = 1L;
        Long authorId = 1L;
        Long viewerId = 99L;
        given(postPersistencePort.findById(postId))
                .willReturn(Optional.of(createPost(postId, authorId)));
        given(memberQueryUseCase.getMemberProfile(authorId))
                .willReturn(createProfile(authorId));
        given(bookmarkPersistencePort.existsByMemberIdAndPostId(viewerId, postId))
                .willReturn(true);

        // when
        PostDetailReadModel result = postService.getPost(postId, viewerId);

        // then
        assertThat(result.isCurrentUserBookmarked()).isTrue();
    }

    @DisplayName("북마크하지 않은 게시글을 상세 조회하면 북마크 여부가 false 이다")
    @Test
    void getPost_notBookmarked_returnsFalse() {
        // given
        Long postId = 1L;
        Long authorId = 1L;
        Long viewerId = 99L;
        given(postPersistencePort.findById(postId))
                .willReturn(Optional.of(createPost(postId, authorId)));
        given(memberQueryUseCase.getMemberProfile(authorId))
                .willReturn(createProfile(authorId));
        given(bookmarkPersistencePort.existsByMemberIdAndPostId(viewerId, postId))
                .willReturn(false);

        // when
        PostDetailReadModel result = postService.getPost(postId, viewerId);

        // then
        assertThat(result.isCurrentUserBookmarked()).isFalse();
    }

    @DisplayName("게시글 목록을 조회하면 작성자가 보강된 페이지 결과를 반환한다")
    @Test
    void getPosts_success() {
        // given
        PostSearchCommand command = PostSearchCommand.builder()
                .sortType(SortType.HOT)
                .page(0)
                .build();

        PostListReadModel item = PostListReadModel.builder()
                .id(10L).title("제목").category("GENERAL")
                .authorId(1L)
                .createdAt(LocalDateTime.now())
                .build();
        SliceResult<PostListReadModel> page =
                new SliceResult<>(List.of(item), false);

        given(postPersistencePort.findPosts(command)).willReturn(page);
        given(memberQueryUseCase.getMemberProfiles(List.of(1L)))
                .willReturn(List.of(createProfile(1L)));

        // when
        SliceResult<PostListReadModel> result = postService.getPosts(command);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getAuthor()).isNotNull();
        assertThat(result.getContent().get(0).getAuthor().id()).isEqualTo(1L);
    }

    private MemberProfileReadModel createProfile(Long memberId) {
        return MemberProfileReadModel.builder()
                .id(memberId)
                .nickname("테스터")
                .profileImageUrl("http://img/" + memberId)
                .build();
    }

    private Post createPost(Long postId, Long memberId) {
        return Post.builder()
                .id(postId)
                .memberId(memberId)
                .title("테스트 제목")
                .content("테스트 내용")
                .category("GENERAL")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
