package com.example.lolserver.community.application;

import com.example.lolserver.community.application.port.in.ImageCleanupUseCase;
import com.example.lolserver.community.application.port.out.ImagePersistencePort;
import com.example.lolserver.community.application.port.out.ImageStoragePort;
import com.example.lolserver.community.config.CommunityImageProperties;
import com.example.lolserver.community.domain.PostImage;
import com.example.lolserver.community.domain.vo.ImageStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 고아 이미지 정리.
 *
 * <p>버킷을 {@code ListObjects} 로 훑어 DB 와 대조하지 않는다. 객체 수에 비례해 비싼 것도 있지만,
 * 무엇보다 <b>"방금 올라간 정상 파일"과 "고아"를 목록만으로는 구분할 수 없다.</b> DB 가
 * 진실원천이고, 상태와 경과시간만 보면 된다.
 *
 * <p>Spring Batch 를 쓰지 않는 이유: 메타 테이블(V27)이 있긴 하나 하루 수십~수백 행 삭제에
 * 잡 인프라를 얹을 이유가 없다. 다중 인스턴스로 확장되면 Redisson 분산 락(championstats 가
 * 이미 쓰는 인프라)으로 단일 실행만 보장하면 된다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageCleanupService implements ImageCleanupUseCase {

    private final ImagePersistencePort imagePersistencePort;
    private final ImageStoragePort imageStoragePort;
    private final CommunityImageProperties properties;

    @Override
    public void cleanupOrphans() {
        LocalDateTime now = LocalDateTime.now();

        // 업로드 도중 끊김(S3 PUT 실패 / 앱 크래시). 파일이 아예 없을 수 있지만
        // DeleteObject 는 멱등이라 그냥 지우면 된다.
        cleanup(ImageStatus.UPLOADING, now.minusHours(properties.getUploadingRetentionHours()));
        // 글을 쓰다 이탈.
        cleanup(ImageStatus.PENDING, now.minusHours(properties.getPendingRetentionHours()));
        // 글 삭제 / 수정으로 본문에서 빠짐.
        cleanup(ImageStatus.DETACHED, now.minusDays(properties.getDetachedRetentionDays()));
    }

    /**
     * <b>스토리지를 먼저 지우고 row 를 나중에 지운다.</b> 업로드(INSERT 선행)와 같은 이유로
     * 방향만 반대다 — row 를 먼저 지우면 S3 삭제가 실패했을 때 파일이 추적 불가능한 고아로 남는다.
     * 이 순서면 실패해도 row 가 남아 다음 배치가 재시도한다.
     */
    private void cleanup(ImageStatus status, LocalDateTime threshold) {
        List<PostImage> targets = imagePersistencePort
                .findExpired(status, threshold, properties.getCleanupBatchSize());
        if (targets.isEmpty()) {
            return;
        }

        try {
            imageStoragePort.deleteAll(targets.stream().map(PostImage::getStorageKey).toList());
        } catch (RuntimeException e) {
            // 다음 실행에서 같은 대상을 다시 잡는다. 여기서 row 를 지우면 파일만 남는다.
            log.error("고아 이미지 스토리지 삭제 실패, 이번 회차는 건너뛴다: status={}, count={}",
                    status, targets.size(), e);
            return;
        }

        imagePersistencePort.deleteAllByIds(targets.stream().map(PostImage::getId).toList());
        log.info("고아 이미지 정리: status={}, count={}", status, targets.size());
    }
}
