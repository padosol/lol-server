package com.example.lolserver.community.adapter.out.persistence;

import com.example.lolserver.common.test.RepositoryTestBase;
import com.example.lolserver.community.adapter.out.persistence.adapter.ImagePersistenceAdapter;
import com.example.lolserver.community.adapter.out.persistence.entity.CommunityImageEntity;
import com.example.lolserver.community.adapter.out.persistence.repository.CommunityImageJpaRepository;
import com.example.lolserver.community.domain.PostImage;
import com.example.lolserver.community.domain.vo.ImageStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 정리 배치의 스캔은 파생 쿼리 이름
 * ({@code findAllByStatusAndUpdatedAtBefore})에 얹혀 있다. 이 이름은 컴파일이 아니라
 * 리포지토리 부트스트랩 시점에 파싱되므로, 이 테스트가 없으면 이름을 잘못 써도 CI 는 green 이고
 * 운영 기동에서 처음 터진다(카테고리 어댑터 테스트와 같은 이유).
 *
 * <p>enum ↔ String 매핑도 여기서만 실제로 검증된다 — MapStruct 가 생성한 코드가
 * {@code valueOf}/{@code name} 을 제대로 걸었는지는 왕복시켜 봐야 안다.
 */
class ImagePersistenceAdapterTest extends RepositoryTestBase {

    @Autowired
    private ImagePersistenceAdapter imagePersistenceAdapter;

    @Autowired
    private CommunityImageJpaRepository imageJpaRepository;

    @DisplayName("도메인 enum 상태가 문자열 컬럼으로 왕복한다")
    @Test
    void save_roundTripsStatusEnum() {
        PostImage saved = imagePersistenceAdapter.save(
                PostImage.uploading(1L, "local/community/2026/08/a.jpg",
                        "https://cdn/local/community/2026/08/a.jpg",
                        "image/jpeg", 100L, 80, 60));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(ImageStatus.UPLOADING);
        assertThat(imageJpaRepository.findById(saved.getId()))
                .get()
                .extracting(CommunityImageEntity::getStatus)
                .isEqualTo("UPLOADING");
    }

    @DisplayName("유예가 지난 행만 상태별로 걸러 온다")
    @Test
    void findExpired_filtersByStatusAndUpdatedAt() {
        LocalDateTime old = LocalDateTime.now().minusDays(3);
        persist("k-old-pending", ImageStatus.PENDING, old);
        persist("k-new-pending", ImageStatus.PENDING, LocalDateTime.now());
        persist("k-old-attached", ImageStatus.ATTACHED, old);

        List<PostImage> expired = imagePersistenceAdapter.findExpired(
                ImageStatus.PENDING, LocalDateTime.now().minusDays(1), 100);

        assertThat(expired).hasSize(1);
        assertThat(expired.get(0).getStorageKey()).isEqualTo("k-old-pending");
    }

    @DisplayName("limit 을 넘겨 한 번에 무는 양을 제한할 수 있다")
    @Test
    void findExpired_respectsLimit() {
        LocalDateTime old = LocalDateTime.now().minusDays(3);
        persist("k-1", ImageStatus.PENDING, old);
        persist("k-2", ImageStatus.PENDING, old);
        persist("k-3", ImageStatus.PENDING, old);

        assertThat(imagePersistenceAdapter.findExpired(
                ImageStatus.PENDING, LocalDateTime.now().minusDays(1), 2)).hasSize(2);
    }

    @DisplayName("게시글별 조회는 post_id 로 묶어 온다")
    @Test
    void findByPostId_returnsOnlyThatPost() {
        persistAttached("k-a", 10L);
        persistAttached("k-b", 10L);
        persistAttached("k-c", 11L);

        assertThat(imagePersistenceAdapter.findByPostId(10L)).hasSize(2);
    }

    @DisplayName("빈 id 목록은 쿼리 없이 빈 결과다")
    @Test
    void findAllByIds_empty_returnsEmpty() {
        assertThat(imagePersistenceAdapter.findAllByIds(List.of())).isEmpty();
    }

    @DisplayName("id 목록으로 일괄 삭제한다")
    @Test
    void deleteAllByIds_removesRows() {
        PostImage first = persist("k-del-1", ImageStatus.PENDING, LocalDateTime.now());
        PostImage second = persist("k-del-2", ImageStatus.PENDING, LocalDateTime.now());

        imagePersistenceAdapter.deleteAllByIds(List.of(first.getId(), second.getId()));

        assertThat(imageJpaRepository.count()).isZero();
    }

    private PostImage persist(String storageKey, ImageStatus status, LocalDateTime updatedAt) {
        CommunityImageEntity entity = imageJpaRepository.save(CommunityImageEntity.builder()
                .memberId(1L)
                .storageKey(storageKey)
                .url("https://cdn/" + storageKey)
                .contentType("image/jpeg")
                .sizeBytes(100L)
                .status(status.name())
                .createdAt(updatedAt)
                .updatedAt(updatedAt)
                .build());
        return imagePersistenceAdapter.findById(entity.getId()).orElseThrow();
    }

    private void persistAttached(String storageKey, Long postId) {
        imageJpaRepository.save(CommunityImageEntity.builder()
                .memberId(1L)
                .postId(postId)
                .storageKey(storageKey)
                .url("https://cdn/" + storageKey)
                .contentType("image/jpeg")
                .sizeBytes(100L)
                .status(ImageStatus.ATTACHED.name())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());
    }
}
