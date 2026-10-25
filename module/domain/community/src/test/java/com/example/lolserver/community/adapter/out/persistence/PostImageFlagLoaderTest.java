package com.example.lolserver.community.adapter.out.persistence;

import com.example.lolserver.common.test.RepositoryTestBase;
import com.example.lolserver.community.adapter.out.persistence.entity.CommunityImageEntity;
import com.example.lolserver.community.adapter.out.persistence.repository.CommunityImageJpaRepository;
import com.example.lolserver.community.adapter.out.persistence.support.PostImageFlagLoader;
import com.example.lolserver.community.application.model.readmodel.PostListReadModel;
import com.example.lolserver.community.domain.vo.ImageStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

/**
 * 목록의 사진 아이콘 판정.
 *
 * <p>{@code @Query} 는 컴파일이 아니라 리포지토리 부트스트랩 시점에 파싱된다. 이 테스트가
 * 없으면 쿼리가 깨져도 CI 는 green 이고 운영 기동에서 처음 터진다.
 *
 * <p>상태 조건이 특히 조용히 틀린다 — 상태를 안 보면 올리다 만 이미지까지 세어
 * <b>사진 없는 글에 아이콘이 붙는다</b>. 컴파일도 되고 예외도 안 난다.
 */
@Import(PostImageFlagLoader.class)
class PostImageFlagLoaderTest extends RepositoryTestBase {

    @Autowired
    private PostImageFlagLoader postImageFlagLoader;

    @Autowired
    private CommunityImageJpaRepository imageJpaRepository;

    @DisplayName("이미지가 붙은 글에만 표시가 남는다")
    @Test
    void markPostsWithImage_marksOnlyPostsWithAttachedImage() {
        persistImage("k-1", 1L, ImageStatus.ATTACHED);

        List<PostListReadModel> marked =
                postImageFlagLoader.markPostsWithImage(List.of(post(1L), post(2L)));

        assertThat(marked).extracting(PostListReadModel::getId, PostListReadModel::isHasImage)
                .containsExactly(
                        tuple(1L, true),
                        tuple(2L, false));
    }

    @DisplayName("본문에 보이지 않는 상태의 이미지는 세지 않는다")
    @Test
    void markPostsWithImage_ignoresNonAttachedStatuses() {
        // PENDING 은 아직 글에 붙지 않았고, DETACHED 는 글에서 빠진 것이다.
        // 둘 다 본문에 안 보이므로 아이콘이 붙으면 거짓말이 된다.
        persistImage("k-pending", 1L, ImageStatus.PENDING);
        persistImage("k-uploading", 2L, ImageStatus.UPLOADING);
        persistImage("k-detached", 3L, ImageStatus.DETACHED);

        List<PostListReadModel> marked = postImageFlagLoader.markPostsWithImage(
                List.of(post(1L), post(2L), post(3L)));

        assertThat(marked).extracting(PostListReadModel::isHasImage)
                .containsOnly(false);
    }

    @DisplayName("한 글에 여러 장이 붙어도 목록 건수는 그대로다")
    @Test
    void markPostsWithImage_keepsOneRowPerPost() {
        persistImage("k-a", 1L, ImageStatus.ATTACHED);
        persistImage("k-b", 1L, ImageStatus.ATTACHED);

        List<PostListReadModel> marked =
                postImageFlagLoader.markPostsWithImage(List.of(post(1L)));

        assertThat(marked).hasSize(1);
        assertThat(marked.get(0).isHasImage()).isTrue();
    }

    @DisplayName("빈 목록은 그대로 돌려준다")
    @Test
    void markPostsWithImage_empty_returnsEmpty() {
        // IN () 은 일부 DB 가 문법 오류로 거절한다. 빈 목록에 아예 묻지 않는다.
        assertThat(postImageFlagLoader.markPostsWithImage(List.of())).isEmpty();
    }

    @DisplayName("다른 글에 붙은 이미지는 넘어오지 않는다")
    @Test
    void markPostsWithImage_ignoresOtherPosts() {
        persistImage("k-other", 99L, ImageStatus.ATTACHED);

        List<PostListReadModel> marked =
                postImageFlagLoader.markPostsWithImage(List.of(post(1L)));

        assertThat(marked.get(0).isHasImage()).isFalse();
    }

    private PostListReadModel post(Long id) {
        return PostListReadModel.builder()
                .id(id)
                .title("글 " + id)
                .categoryId(1L)
                .authorId(1L)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private void persistImage(String storageKey, Long postId, ImageStatus status) {
        imageJpaRepository.save(CommunityImageEntity.builder()
                .memberId(1L)
                .postId(postId)
                .storageKey(storageKey)
                .url("https://cdn/" + storageKey)
                .contentType("image/png")
                .sizeBytes(100L)
                .status(status.name())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());
    }
}
