package com.example.lolserver.community.application;

import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
import com.example.lolserver.common.support.SliceResult;
import com.example.lolserver.community.application.model.readmodel.PostListReadModel;
import com.example.lolserver.community.application.port.out.BookmarkPersistencePort;
import com.example.lolserver.community.application.port.out.PostPersistencePort;
import com.example.lolserver.community.domain.Bookmark;
import com.example.lolserver.community.domain.Post;
import com.example.lolserver.member.application.model.readmodel.MemberProfileReadModel;
import com.example.lolserver.member.application.port.in.MemberQueryUseCase;
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
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class BookmarkServiceTest {

    @Mock
    private BookmarkPersistencePort bookmarkPersistencePort;

    @Mock
    private PostPersistencePort postPersistencePort;

    @Mock
    private MemberQueryUseCase memberQueryUseCase;

    @InjectMocks
    private BookmarkService bookmarkService;

    @DisplayName("북마크하지 않은 게시글을 북마크하면 저장된다")
    @Test
    void addBookmark_success() {
        // given
        Long memberId = 1L;
        Long postId = 10L;
        given(postPersistencePort.findById(postId))
                .willReturn(Optional.of(createPost(postId, false)));
        given(bookmarkPersistencePort.existsByMemberIdAndPostId(memberId, postId))
                .willReturn(false);

        // when
        bookmarkService.addBookmark(memberId, postId);

        // then
        then(bookmarkPersistencePort).should().save(any(Bookmark.class));
    }

    @DisplayName("이미 북마크한 게시글을 다시 북마크하면 예외가 발생한다")
    @Test
    void addBookmark_alreadyExists() {
        // given
        Long memberId = 1L;
        Long postId = 10L;
        given(postPersistencePort.findById(postId))
                .willReturn(Optional.of(createPost(postId, false)));
        given(bookmarkPersistencePort.existsByMemberIdAndPostId(memberId, postId))
                .willReturn(true);

        // when & then
        assertThatThrownBy(() -> bookmarkService.addBookmark(memberId, postId))
                .isInstanceOf(CoreException.class)
                .extracting(e -> ((CoreException) e).getErrorType())
                .isEqualTo(ErrorType.BOOKMARK_ALREADY_EXISTS);
        then(bookmarkPersistencePort).should(never()).save(any());
    }

    @DisplayName("존재하지 않는 게시글을 북마크하면 예외가 발생한다")
    @Test
    void addBookmark_postNotFound() {
        // given
        Long memberId = 1L;
        Long postId = 999L;
        given(postPersistencePort.findById(postId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> bookmarkService.addBookmark(memberId, postId))
                .isInstanceOf(CoreException.class)
                .extracting(e -> ((CoreException) e).getErrorType())
                .isEqualTo(ErrorType.POST_NOT_FOUND);
        then(bookmarkPersistencePort).should(never()).save(any());
    }

    @DisplayName("삭제된 게시글을 북마크하면 예외가 발생한다")
    @Test
    void addBookmark_deletedPost() {
        // given
        Long memberId = 1L;
        Long postId = 10L;
        given(postPersistencePort.findById(postId))
                .willReturn(Optional.of(createPost(postId, true)));

        // when & then
        assertThatThrownBy(() -> bookmarkService.addBookmark(memberId, postId))
                .isInstanceOf(CoreException.class)
                .extracting(e -> ((CoreException) e).getErrorType())
                .isEqualTo(ErrorType.POST_NOT_FOUND);
        then(bookmarkPersistencePort).should(never()).save(any());
    }

    @DisplayName("북마크한 게시글을 해제하면 삭제된다")
    @Test
    void removeBookmark_success() {
        // given
        Long memberId = 1L;
        Long postId = 10L;
        Bookmark bookmark = Bookmark.create(memberId, postId);
        given(bookmarkPersistencePort.findByMemberIdAndPostId(memberId, postId))
                .willReturn(Optional.of(bookmark));

        // when
        bookmarkService.removeBookmark(memberId, postId);

        // then
        then(bookmarkPersistencePort).should().delete(bookmark);
    }

    @DisplayName("북마크하지 않은 게시글을 해제하면 예외가 발생한다")
    @Test
    void removeBookmark_notFound() {
        // given
        Long memberId = 1L;
        Long postId = 10L;
        given(bookmarkPersistencePort.findByMemberIdAndPostId(memberId, postId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> bookmarkService.removeBookmark(memberId, postId))
                .isInstanceOf(CoreException.class)
                .extracting(e -> ((CoreException) e).getErrorType())
                .isEqualTo(ErrorType.BOOKMARK_NOT_FOUND);
    }

    @DisplayName("삭제된 게시글의 북마크는 해제할 수 있다")
    @Test
    void removeBookmark_deletedPost_allowed() {
        // given — 글이 지워졌다고 내 북마크가 남아있으면 안 되므로 해제는 막지 않는다
        Long memberId = 1L;
        Long postId = 10L;
        Bookmark bookmark = Bookmark.create(memberId, postId);
        given(bookmarkPersistencePort.findByMemberIdAndPostId(memberId, postId))
                .willReturn(Optional.of(bookmark));

        // when
        bookmarkService.removeBookmark(memberId, postId);

        // then
        then(bookmarkPersistencePort).should().delete(bookmark);
        then(postPersistencePort).shouldHaveNoInteractions();
    }

    @DisplayName("내 북마크 목록을 조회하면 작성자가 보강된 페이지 결과를 반환한다")
    @Test
    void getMyBookmarks_enrichesAuthor() {
        // given — 영속성은 authorId 만 채워주고 author 는 비어 있다
        Long memberId = 1L;
        int page = 2;
        PostListReadModel item = PostListReadModel.builder()
                .id(10L).title("제목").category("GENERAL")
                .authorId(7L)
                .createdAt(LocalDateTime.now())
                .build();
        given(bookmarkPersistencePort.findBookmarkedPosts(memberId, page))
                .willReturn(new SliceResult<>(List.of(item), true));
        given(memberQueryUseCase.getMemberProfiles(List.of(7L)))
                .willReturn(List.of(MemberProfileReadModel.builder()
                        .id(7L).nickname("테스터")
                        .profileImageUrl("http://img/7").build()));

        // when
        SliceResult<PostListReadModel> result =
                bookmarkService.getMyBookmarks(memberId, page);

        // then — 게시글 목록과 동일하게 author 가 채워져야 한다.
        // 안 채우면 북마크 목록에서만 작성자가 비어 보인다.
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getAuthor()).isNotNull();
        assertThat(result.getContent().get(0).getAuthor().id()).isEqualTo(7L);
        assertThat(result.getContent().get(0).getAuthor().nickname()).isEqualTo("테스터");
        assertThat(result.isHasNext()).isTrue();
    }

    @DisplayName("북마크 목록이 비어 있으면 회원 조회를 하지 않는다")
    @Test
    void getMyBookmarks_empty_skipsMemberLookup() {
        // given
        given(bookmarkPersistencePort.findBookmarkedPosts(1L, 0))
                .willReturn(new SliceResult<>(List.of(), false));

        // when
        SliceResult<PostListReadModel> result = bookmarkService.getMyBookmarks(1L, 0);

        // then — 빈 목록에 회원 조회를 날리면 불필요한 쿼리가 된다
        assertThat(result.getContent()).isEmpty();
        then(memberQueryUseCase).shouldHaveNoInteractions();
    }

    @DisplayName("page 가 음수면 500 이 아니라 INVALID_INPUT 으로 막는다")
    @Test
    void getMyBookmarks_negativePage() {
        // when & then — PageRequest.of 까지 내려가면 IllegalArgumentException 으로 500 이 된다
        assertThatThrownBy(() -> bookmarkService.getMyBookmarks(1L, -1))
                .isInstanceOf(CoreException.class)
                .extracting(e -> ((CoreException) e).getErrorType())
                .isEqualTo(ErrorType.INVALID_INPUT);
        then(bookmarkPersistencePort).shouldHaveNoInteractions();
    }

    private Post createPost(Long postId, boolean deleted) {
        return Post.builder()
                .id(postId)
                .memberId(2L)
                .title("제목")
                .content("내용")
                .category("GENERAL")
                .deleted(deleted)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
