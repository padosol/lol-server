package com.example.lolserver.community.application;

import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
import com.example.lolserver.community.application.command.UploadImageCommand;
import com.example.lolserver.community.application.model.readmodel.PostImageReadModel;
import com.example.lolserver.community.application.port.in.ImageAttachUseCase;
import com.example.lolserver.community.application.port.in.ImageQueryUseCase;
import com.example.lolserver.community.application.port.in.ImageUseCase;
import com.example.lolserver.community.application.port.out.ImagePersistencePort;
import com.example.lolserver.community.application.port.out.ImageProcessorPort;
import com.example.lolserver.community.application.port.out.ImageRateLimitPort;
import com.example.lolserver.community.application.port.out.ImageStoragePort;
import com.example.lolserver.community.application.port.out.ProcessedImage;
import com.example.lolserver.community.application.port.out.StoredImageLocation;
import com.example.lolserver.community.config.CommunityImageProperties;
import com.example.lolserver.community.domain.PostImage;
import com.example.lolserver.community.domain.vo.ImageStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 커뮤니티 이미지의 업로드·삭제·첨부 확정.
 *
 * <p><b>클래스 레벨에 {@code @Transactional} 을 걸지 않는다.</b> 업로드 경로가 S3 호출을
 * 포함하기 때문이다. 외부 네트워크 호출이 트랜잭션을 잡고 있으면 S3 지연이 그대로 DB 커넥션
 * 점유 시간이 되고, 업로드가 몰릴 때 커넥션 풀이 먼저 마른다. 대신 첨부 확정처럼 순수 DB
 * 작업인 메서드에만 개별적으로 붙인다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageService implements ImageUseCase, ImageAttachUseCase, ImageQueryUseCase {

    private final ImagePersistencePort imagePersistencePort;
    private final ImageStoragePort imageStoragePort;
    private final ImageProcessorPort imageProcessorPort;
    private final ImageRateLimitPort imageRateLimitPort;
    private final CommunityImageProperties properties;

    /**
     * <pre>
     *   1. rate limit        ← 가장 먼저. 디코드·리사이즈가 이 파이프라인에서 제일 비싸다
     *   2. 용량 검증
     *   3~5. 타입 판별 + 폭탄 가드 + 리사이즈/EXIF 제거   (ImageProcessorPort)
     *   6. 키 발급 + DB INSERT(UPLOADING)     ← S3 PUT '전에'
     *   7. S3 PUT                             ← 트랜잭션 밖
     *   8. DB UPDATE(PENDING)
     * </pre>
     *
     * <p>6 이 7 보다 먼저인 이유: 반대 순서면 PUT 성공 후 INSERT 가 실패한 파일이 DB 에 흔적
     * 없이 버킷에만 남아 정리 배치가 영영 찾지 못한다. 이 순서라면 DB 가 항상 S3 의 상위집합이라
     * 모든 파일이 추적된다. 대가는 DB 왕복 1회 추가인데 업로드 빈도를 생각하면 무시할 수준이다.
     */
    @Override
    public PostImageReadModel upload(Long memberId, UploadImageCommand command) {
        if (!imageRateLimitPort.tryAcquire(memberId, properties.getUploadRatePerMinute())) {
            throw new CoreException(ErrorType.IMAGE_UPLOAD_RATE_LIMITED);
        }
        validateSize(command.getContent());

        ProcessedImage processed = imageProcessorPort.process(command.getContent());
        StoredImageLocation location = imageStoragePort.allocate(processed.extension());

        PostImage image = imagePersistencePort.save(PostImage.uploading(
                memberId, location.storageKey(), location.url(),
                processed.contentType(), processed.sizeBytes(),
                processed.width(), processed.height()));

        imageStoragePort.store(location.storageKey(), processed.content(), processed.contentType());

        image.markUploaded();
        return PostImageReadModel.of(imagePersistencePort.save(image));
    }

    /** 스토리지를 먼저 지운다 — row 를 먼저 지우면 삭제가 실패했을 때 파일이 추적 불가능해진다. */
    @Override
    public void delete(Long memberId, Long imageId) {
        PostImage image = imagePersistencePort.findById(imageId)
                .orElseThrow(() -> new CoreException(ErrorType.IMAGE_NOT_FOUND));

        image.validateDeletable(memberId);

        imageStoragePort.delete(image.getStorageKey());
        imagePersistencePort.deleteAllByIds(List.of(imageId));
    }

    @Override
    @Transactional
    public void attach(Long memberId, Long postId, List<Long> imageIds) {
        List<Long> targetIds = distinct(imageIds);
        if (targetIds.isEmpty()) {
            return;
        }
        validateCount(targetIds.size());

        List<PostImage> images = imagePersistencePort.findAllByIds(targetIds);
        if (images.size() != targetIds.size()) {
            throw new CoreException(ErrorType.IMAGE_NOT_FOUND);
        }

        images.forEach(image -> image.attachTo(postId, memberId));
        imagePersistencePort.saveAll(images);
    }

    @Override
    @Transactional
    public void replace(Long memberId, Long postId, List<Long> imageIds) {
        List<Long> requested = distinct(imageIds);
        validateCount(requested.size());

        List<PostImage> attached = findAttached(postId);

        // A \ B — 본문에서 빠진 것은 떼어낸다(파일은 유예 후 배치가 지운다).
        List<PostImage> removed = attached.stream()
                .filter(image -> !requested.contains(image.getId()))
                .toList();
        removed.forEach(PostImage::detach);
        imagePersistencePort.saveAll(removed);

        // B \ A — 새로 들어온 것만 붙인다. 이미 ATTACHED 인 것을 다시 붙이면
        // PENDING 만 허용하는 도메인 규칙에 걸린다.
        Set<Long> attachedIds = attached.stream()
                .map(PostImage::getId)
                .collect(Collectors.toSet());
        attach(memberId, postId, requested.stream()
                .filter(id -> !attachedIds.contains(id))
                .toList());
    }

    @Override
    @Transactional
    public void detachByPostId(Long postId) {
        List<PostImage> attached = findAttached(postId);
        attached.forEach(PostImage::detach);
        imagePersistencePort.saveAll(attached);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostImageReadModel> getPostImages(Long postId) {
        return findAttached(postId).stream()
                .map(PostImageReadModel::of)
                .toList();
    }

    /**
     * ATTACHED 만 본다. DETACHED 는 이력으로 {@code post_id} 를 유지하고 있어
     * 필터 없이 조회하면 이미 뗀 이미지가 첨부 목록에 다시 나타난다.
     */
    private List<PostImage> findAttached(Long postId) {
        return imagePersistencePort.findByPostId(postId).stream()
                .filter(image -> image.getStatus() == ImageStatus.ATTACHED)
                .toList();
    }

    private void validateSize(byte[] content) {
        if (content == null || content.length == 0) {
            throw new CoreException(ErrorType.IMAGE_FILE_REQUIRED);
        }
        if (content.length > properties.getMaxSizeBytes()) {
            throw new CoreException(ErrorType.IMAGE_SIZE_EXCEEDED);
        }
    }

    private void validateCount(int count) {
        if (count > properties.getMaxCountPerPost()) {
            throw new CoreException(ErrorType.IMAGE_COUNT_EXCEEDED);
        }
    }

    private List<Long> distinct(List<Long> imageIds) {
        if (imageIds == null) {
            return List.of();
        }
        return imageIds.stream().filter(Objects::nonNull).distinct().toList();
    }
}
