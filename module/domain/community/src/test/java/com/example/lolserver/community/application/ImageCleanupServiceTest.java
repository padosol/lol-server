package com.example.lolserver.community.application;

import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
import com.example.lolserver.community.application.port.out.ImagePersistencePort;
import com.example.lolserver.community.application.port.out.ImageStoragePort;
import com.example.lolserver.community.config.CommunityImageProperties;
import com.example.lolserver.community.domain.PostImage;
import com.example.lolserver.community.domain.vo.ImageStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ImageCleanupServiceTest {

    @Mock
    private ImagePersistencePort imagePersistencePort;

    @Mock
    private ImageStoragePort imageStoragePort;

    private ImageCleanupService imageCleanupService;

    @BeforeEach
    void setUp() {
        imageCleanupService = new ImageCleanupService(imagePersistencePort, imageStoragePort,
                new CommunityImageProperties());
    }

    @DisplayName("UPLOADING·PENDING·DETACHED 세 상태를 각각 다른 유예로 스캔한다")
    @Test
    void cleanupOrphans_scansThreeStatuses() {
        given(imagePersistencePort.findExpired(any(), any(), anyInt())).willReturn(List.of());

        imageCleanupService.cleanupOrphans();

        ArgumentCaptor<ImageStatus> statuses = ArgumentCaptor.forClass(ImageStatus.class);
        then(imagePersistencePort).should(org.mockito.Mockito.times(3))
                .findExpired(statuses.capture(), any(), anyInt());
        assertThat(statuses.getAllValues()).containsExactly(
                ImageStatus.UPLOADING, ImageStatus.PENDING, ImageStatus.DETACHED);
    }

    @DisplayName("DETACHED 유예는 created_at 이 아니라 updated_at 기준이라 떨어져 나온 시점부터 센다")
    @Test
    void cleanupOrphans_usesDetachedRetentionDays() {
        CommunityImageProperties properties = new CommunityImageProperties();
        properties.setDetachedRetentionDays(7);
        ImageCleanupService service = new ImageCleanupService(
                imagePersistencePort, imageStoragePort, properties);
        given(imagePersistencePort.findExpired(any(), any(), anyInt())).willReturn(List.of());

        LocalDateTime before = LocalDateTime.now();
        service.cleanupOrphans();

        ArgumentCaptor<LocalDateTime> thresholds = ArgumentCaptor.forClass(LocalDateTime.class);
        then(imagePersistencePort).should(org.mockito.Mockito.times(3))
                .findExpired(any(), thresholds.capture(), anyInt());
        // 서비스가 before 이후에 now() 를 잡으므로 임계값은 before-7d 이상이어야 한다.
        assertThat(thresholds.getAllValues().get(2))
                .isAfterOrEqualTo(before.minusDays(7));
    }

    @DisplayName("스토리지를 먼저 지우고 row 를 지운다")
    @Test
    void cleanupOrphans_deletesStorageBeforeRows() {
        given(imagePersistencePort.findExpired(any(ImageStatus.class), any(), anyInt()))
                .willReturn(List.of(expired(1L, "key-1")), List.of(), List.of());

        imageCleanupService.cleanupOrphans();

        InOrder order = inOrder(imageStoragePort, imagePersistencePort);
        order.verify(imageStoragePort).deleteAll(List.of("key-1"));
        order.verify(imagePersistencePort).deleteAllByIds(List.of(1L));
    }

    @DisplayName("스토리지 삭제가 실패하면 row 를 남긴다 — 다음 배치가 재시도해야 파일이 안 남는다")
    @Test
    void cleanupOrphans_storageFails_keepsRows() {
        given(imagePersistencePort.findExpired(any(ImageStatus.class), any(), anyInt()))
                .willReturn(List.of(expired(1L, "key-1")), List.of(), List.of());
        willThrow(new CoreException(ErrorType.IMAGE_STORAGE_FAILED))
                .given(imageStoragePort).deleteAll(anyList());

        imageCleanupService.cleanupOrphans();

        then(imagePersistencePort).should(never()).deleteAllByIds(anyList());
    }

    @DisplayName("대상이 없으면 스토리지를 호출하지 않는다")
    @Test
    void cleanupOrphans_noTargets_skipsStorage() {
        given(imagePersistencePort.findExpired(any(), any(), anyInt())).willReturn(List.of());

        imageCleanupService.cleanupOrphans();

        then(imageStoragePort).should(never()).deleteAll(anyList());
    }

    private static PostImage expired(Long id, String storageKey) {
        return PostImage.builder()
                .id(id)
                .memberId(1L)
                .storageKey(storageKey)
                .url("https://cdn.example.com/" + storageKey)
                .contentType("image/jpeg")
                .sizeBytes(4L)
                .status(ImageStatus.PENDING)
                .build();
    }
}
