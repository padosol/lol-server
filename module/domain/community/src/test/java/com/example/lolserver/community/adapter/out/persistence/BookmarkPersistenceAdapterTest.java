package com.example.lolserver.community.adapter.out.persistence;

import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
import com.example.lolserver.common.support.SliceResult;
import com.example.lolserver.common.test.RepositoryTestBase;
import com.example.lolserver.community.adapter.out.persistence.adapter.BookmarkPersistenceAdapter;
import com.example.lolserver.community.adapter.out.persistence.entity.CommunityPostEntity;
import com.example.lolserver.community.adapter.out.persistence.repository.CommunityPostJpaRepository;
import com.example.lolserver.community.application.model.readmodel.PostListReadModel;
import com.example.lolserver.community.domain.Bookmark;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JPQL 조인 쿼리와 UNIQUE 제약은 단위 테스트로 검증할 수 없다.
 * @Query 는 컴파일이 아니라 리포지토리 부트스트랩 시점에 파싱되므로,
 * 이 테스트가 없으면 쿼리가 깨져도 CI 는 green 이고 운영 기동에서 처음 터진다.
 */
class BookmarkPersistenceAdapterTest extends RepositoryTestBase {

    @Autowired
    private BookmarkPersistenceAdapter bookmarkPersistenceAdapter;

    @Autowired
    private CommunityPostJpaRepository postJpaRepository;

    @DisplayName("북마크한 글 목록은 글 작성 시각이 아니라 북마크한 시각의 최신순으로 나온다")
    @Test
    void findBookmarkedPosts_orderedByBookmarkedAt() {
        // given — 글은 오래된 순으로 만들고, 북마크는 그 반대 순으로 담는다
        Long memberId = 1L;
        Long oldPostId = savePost("오래된 글", false);
        Long newPostId = savePost("새 글", false);

        bookmarkAt(memberId, newPostId, LocalDateTime.now().minusDays(2));
        bookmarkAt(memberId, oldPostId, LocalDateTime.now().minusHours(1));

        // when
        SliceResult<PostListReadModel> result =
                bookmarkPersistenceAdapter.findBookmarkedPosts(memberId, 0);

        // then — 최근에 담은 "오래된 글" 이 먼저 나와야 한다
        assertThat(result.getContent()).extracting(PostListReadModel::getTitle)
                .containsExactly("오래된 글", "새 글");
    }

    @DisplayName("삭제된 글은 북마크 목록에서 제외된다")
    @Test
    void findBookmarkedPosts_excludesDeletedPost() {
        // given
        Long memberId = 1L;
        Long alive = savePost("살아있는 글", false);
        Long deleted = savePost("삭제된 글", true);
        bookmarkAt(memberId, alive, LocalDateTime.now());
        bookmarkAt(memberId, deleted, LocalDateTime.now());

        // when
        SliceResult<PostListReadModel> result =
                bookmarkPersistenceAdapter.findBookmarkedPosts(memberId, 0);

        // then
        assertThat(result.getContent()).extracting(PostListReadModel::getTitle)
                .containsExactly("살아있는 글");
    }

    @DisplayName("다른 회원의 북마크는 섞이지 않는다")
    @Test
    void findBookmarkedPosts_isolatedPerMember() {
        // given
        Long postId = savePost("공용 글", false);
        bookmarkAt(1L, postId, LocalDateTime.now());

        // when
        SliceResult<PostListReadModel> result =
                bookmarkPersistenceAdapter.findBookmarkedPosts(2L, 0);

        // then
        assertThat(result.getContent()).isEmpty();
    }

    @DisplayName("페이지 크기(20)를 넘으면 hasNext 가 true 이고 초과분은 잘린다")
    @Test
    void findBookmarkedPosts_hasNext() {
        // given — 21건
        Long memberId = 1L;
        for (int i = 0; i < 21; i++) {
            bookmarkAt(memberId, savePost("글 " + i, false),
                    LocalDateTime.now().minusMinutes(i));
        }

        // when
        SliceResult<PostListReadModel> first =
                bookmarkPersistenceAdapter.findBookmarkedPosts(memberId, 0);
        SliceResult<PostListReadModel> second =
                bookmarkPersistenceAdapter.findBookmarkedPosts(memberId, 1);

        // then
        assertThat(first.getContent()).hasSize(20);
        assertThat(first.isHasNext()).isTrue();
        assertThat(second.getContent()).hasSize(1);
        assertThat(second.isHasNext()).isFalse();
    }

    @DisplayName("같은 회원이 같은 글을 두 번 북마크하면 BOOKMARK_ALREADY_EXISTS 로 변환된다")
    @Test
    void save_duplicate_translatesToDomainError() {
        // given — 서비스의 사전 조회를 통과한 동시 요청 두 건과 같은 상황
        Long memberId = 1L;
        Long postId = savePost("글", false);
        bookmarkPersistenceAdapter.save(Bookmark.create(memberId, postId));

        // when & then — DataIntegrityViolationException 이 그대로 새어나가면 500 이 된다
        assertThatThrownBy(() ->
                bookmarkPersistenceAdapter.save(Bookmark.create(memberId, postId)))
                .isInstanceOf(CoreException.class)
                .extracting(e -> ((CoreException) e).getErrorType())
                .isEqualTo(ErrorType.BOOKMARK_ALREADY_EXISTS);
    }

    @DisplayName("다른 회원이 같은 글을 북마크하는 것은 허용된다")
    @Test
    void save_differentMemberSamePost_allowed() {
        // given
        Long postId = savePost("글", false);
        bookmarkPersistenceAdapter.save(Bookmark.create(1L, postId));

        // when
        Bookmark saved = bookmarkPersistenceAdapter.save(Bookmark.create(2L, postId));

        // then
        assertThat(saved.getId()).isNotNull();
    }

    private Long savePost(String title, boolean deleted) {
        LocalDateTime now = LocalDateTime.now();
        return postJpaRepository.save(CommunityPostEntity.builder()
                .memberId(99L)
                .title(title)
                .content("내용")
                .categoryId(1L)
                .deleted(deleted)
                .createdAt(now)
                .updatedAt(now)
                .build()).getId();
    }

    private void bookmarkAt(Long memberId, Long postId, LocalDateTime bookmarkedAt) {
        bookmarkPersistenceAdapter.save(Bookmark.builder()
                .memberId(memberId)
                .postId(postId)
                .createdAt(bookmarkedAt)
                .build());
    }
}
