package com.example.lolserver.community.domain;

import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
import com.example.lolserver.community.domain.vo.ImageStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 같은 컨텍스트의 {@code PostTest}·{@code CommentTest} 와 같이 평면 구조로 둔다.
 * 그룹핑은 {@code @DisplayName} 문장으로 충분하다.
 */
class PostImageTest {

    private static final Long OWNER_ID = 1L;
    private static final Long OTHER_ID = 2L;
    private static final Long POST_ID = 10L;

    @DisplayName("최초 상태는 UPLOADING 이다 — S3 PUT 전에 행이 먼저 생기기 때문")
    @Test
    void uploading_isInitialStatus() {
        PostImage image = createUploading();

        assertThat(image.getStatus()).isEqualTo(ImageStatus.UPLOADING);
        assertThat(image.getPostId()).isNull();
    }

    @DisplayName("PUT 성공을 알리면 PENDING 으로 넘어간다")
    @Test
    void markUploaded_movesToPending() {
        PostImage image = createUploading();

        image.markUploaded();

        assertThat(image.getStatus()).isEqualTo(ImageStatus.PENDING);
    }

    @DisplayName("이미 PENDING 인 이미지를 다시 업로드 완료 처리할 수 없다")
    @Test
    void markUploaded_twice_throws() {
        PostImage image = createUploading();
        image.markUploaded();

        assertThatThrownBy(image::markUploaded)
                .isInstanceOf(CoreException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.IMAGE_NOT_ATTACHABLE);
    }

    @DisplayName("PENDING 이미지를 본인 글에 붙이면 ATTACHED 가 되고 post_id 가 채워진다")
    @Test
    void attachTo_success() {
        PostImage image = createPending();

        image.attachTo(POST_ID, OWNER_ID);

        assertThat(image.getStatus()).isEqualTo(ImageStatus.ATTACHED);
        assertThat(image.getPostId()).isEqualTo(POST_ID);
    }

    @DisplayName("남의 이미지는 내 글에 붙일 수 없다")
    @Test
    void attachTo_otherMembersImage_throws() {
        PostImage image = createPending();

        assertThatThrownBy(() -> image.attachTo(POST_ID, OTHER_ID))
                .isInstanceOf(CoreException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.FORBIDDEN);
    }

    @DisplayName("이미 다른 글에 붙은 이미지는 재사용할 수 없다")
    @Test
    void attachTo_alreadyAttached_throws() {
        PostImage image = createPending();
        image.attachTo(POST_ID, OWNER_ID);

        assertThatThrownBy(() -> image.attachTo(99L, OWNER_ID))
                .isInstanceOf(CoreException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.IMAGE_NOT_ATTACHABLE);
    }

    @DisplayName("업로드가 끝나지 않은 이미지는 붙일 수 없다")
    @Test
    void attachTo_stillUploading_throws() {
        PostImage image = createUploading();

        assertThatThrownBy(() -> image.attachTo(POST_ID, OWNER_ID))
                .isInstanceOf(CoreException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.IMAGE_NOT_ATTACHABLE);
    }

    @DisplayName("떼어내도 post_id 는 이력으로 남는다")
    @Test
    void detach_keepsPostIdAsHistory() {
        PostImage image = createPending();
        image.attachTo(POST_ID, OWNER_ID);

        image.detach();

        assertThat(image.getStatus()).isEqualTo(ImageStatus.DETACHED);
        assertThat(image.getPostId()).isEqualTo(POST_ID);
    }

    @DisplayName("본인의 PENDING 이미지는 즉시 삭제할 수 있다")
    @Test
    void validateDeletable_pendingByOwner_passes() {
        PostImage image = createPending();

        image.validateDeletable(OWNER_ID);
    }

    @DisplayName("글에 붙은 이미지는 삭제 API 로 지울 수 없다 — 글 수정으로만 떨어진다")
    @Test
    void validateDeletable_attached_throws() {
        PostImage image = createPending();
        image.attachTo(POST_ID, OWNER_ID);

        assertThatThrownBy(() -> image.validateDeletable(OWNER_ID))
                .isInstanceOf(CoreException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.IMAGE_NOT_ATTACHABLE);
    }

    @DisplayName("남의 이미지는 삭제할 수 없다")
    @Test
    void validateDeletable_otherMember_throws() {
        PostImage image = createPending();

        assertThatThrownBy(() -> image.validateDeletable(OTHER_ID))
                .isInstanceOf(CoreException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.FORBIDDEN);
    }

    private static PostImage createUploading() {
        return PostImage.uploading(OWNER_ID, "local/community/2026/08/uuid.jpg",
                "https://cdn.example.com/local/community/2026/08/uuid.jpg",
                "image/jpeg", 1024L, 800, 600);
    }

    private static PostImage createPending() {
        PostImage image = createUploading();
        image.markUploaded();
        return image;
    }
}
