package com.example.lolserver.community.domain;

import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
import com.example.lolserver.community.domain.vo.ImageStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 커뮤니티 게시글 본문 이미지.
 *
 * <p>상태 전이 규칙은 이 도메인이 직접 강제한다. 서비스에서 {@code if + throw} 하지 않는 것이
 * 프로젝트 컨벤션이며, 특히 "남의 이미지를 내 글에 붙인다" 같은 도용은 한 군데에서만 막혀야 한다.
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PostImage {

    private Long id;
    private Long memberId;
    /** 첨부된 게시글. PENDING 구간에서는 null 이고, DETACHED 이후에도 이력으로 남긴다. */
    private Long postId;
    /** 스토리지 내 불변 키. CDN 도메인이 바뀌어도 이 값은 바뀌지 않는다. */
    private String storageKey;
    /** 발급 당시 URL 스냅샷(감사·디버깅용). 응답은 storageKey + base-url 로 조합한다. */
    private String url;
    private String contentType;
    private long sizeBytes;
    private Integer width;
    private Integer height;
    private ImageStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * S3 PUT <b>전에</b> 만들어지는 최초 상태.
     *
     * <p>순서를 뒤집어 PUT 을 먼저 하면, PUT 성공 후 INSERT 가 실패한 파일은 DB 에 흔적이 없어
     * 정리 배치가 영영 찾지 못하고 버킷에만 쌓인다. DB 가 항상 S3 의 상위집합이어야 고아 판별이
     * 성립한다.
     */
    public static PostImage uploading(Long memberId, String storageKey, String url,
            String contentType, long sizeBytes, Integer width, Integer height) {
        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
        return PostImage.builder()
                .memberId(memberId)
                .storageKey(storageKey)
                .url(url)
                .contentType(contentType)
                .sizeBytes(sizeBytes)
                .width(width)
                .height(height)
                .status(ImageStatus.UPLOADING)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    /** S3 PUT 성공. 이 시점부터 파일이 실제로 존재한다. */
    public void markUploaded() {
        validateStatus(ImageStatus.UPLOADING);
        this.status = ImageStatus.PENDING;
        touch();
    }

    /**
     * 글 저장 시 확정. S3 재조회 없이 두 컬럼만 UPDATE 한다 — 크기·MIME·width/height 는
     * 업로드 때 서버가 디코드하면서 이미 알고 있던 값이고, 애초에 S3 는 "누가 올렸는지 /
     * 어느 글 소유인지"를 모른다.
     */
    public void attachTo(Long postId, Long requesterId) {
        validateOwner(requesterId);
        // PENDING 만 허용해 이미 다른 글에 붙은 이미지의 재사용을 막는다.
        validateAttachable();
        this.postId = postId;
        this.status = ImageStatus.ATTACHED;
        touch();
    }

    /**
     * 글 삭제 / 수정으로 본문에서 빠짐. 파일은 아직 지우지 않는다 —
     * 글이 soft delete 이므로 복구 여지가 있고, CDN 에 남은 참조가 곧바로 404 를 뿜지 않게 한다.
     */
    public void detach() {
        validateStatus(ImageStatus.ATTACHED);
        this.status = ImageStatus.DETACHED;
        touch();
    }

    /** 에디터에서 사용자가 명시적으로 뺀 경우. 본인 소유 + PENDING 만 즉시 삭제할 수 있다. */
    public void validateDeletable(Long requesterId) {
        validateOwner(requesterId);
        validateStatus(ImageStatus.PENDING);
    }

    public boolean isOwner(Long memberId) {
        return this.memberId.equals(memberId);
    }

    private void validateOwner(Long requesterId) {
        if (!isOwner(requesterId)) {
            throw new CoreException(ErrorType.FORBIDDEN);
        }
    }

    private void validateAttachable() {
        if (this.status != ImageStatus.PENDING) {
            throw new CoreException(ErrorType.IMAGE_NOT_ATTACHABLE);
        }
    }

    private void validateStatus(ImageStatus expected) {
        if (this.status != expected) {
            throw new CoreException(ErrorType.IMAGE_NOT_ATTACHABLE);
        }
    }

    /**
     * 시각은 배포 환경의 기본 존으로 찍는다 — {@code Post}·{@code Comment} 등 커뮤니티의
     * 다른 도메인이 모두 같은 방식이고, DB 컬럼도 존 정보 없는 {@code TIMESTAMP} 다.
     * 여기만 다른 존을 쓰면 같은 화면에 뜨는 글과 이미지의 시각이 어긋나고, 정리 배치가
     * {@code updated_at} 을 기준으로 계산하는 유예기간도 통째로 밀린다.
     * (존을 옮긴다면 이 클래스가 아니라 커뮤니티 전체를 함께 옮겨야 한다.)
     */
    private void touch() {
        this.updatedAt = LocalDateTime.now(ZoneId.systemDefault());
    }
}
