package com.example.lolserver.community.application;

import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
import com.example.lolserver.community.application.port.out.BookmarkPersistencePort;
import com.example.lolserver.community.application.port.out.PostPersistencePort;
import com.example.lolserver.community.domain.Bookmark;
import com.example.lolserver.community.domain.Post;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

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
        given(bookmarkPersistencePort.findByMemberIdAndPostId(memberId, postId))
                .willReturn(Optional.empty());

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
        given(bookmarkPersistencePort.findByMemberIdAndPostId(memberId, postId))
                .willReturn(Optional.of(Bookmark.create(memberId, postId)));

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
