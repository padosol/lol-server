package com.example.lolserver.community.adapter.out.storage;

import com.example.lolserver.common.config.StorageProperties;
import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
import com.example.lolserver.community.application.port.out.ImageStoragePort;
import com.example.lolserver.community.application.port.out.StoredImageLocation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * 유일한 스토리지 구현. 로컬도 운영도 이 코드를 탄다 — 격리는 코드가 아니라 버킷이 한다.
 *
 * <p>로컬만 파일시스템 어댑터를 타면 S3 권한·키 규칙·CDN 캐시·삭제 동작이 운영에 배포한 뒤에야
 * 처음 실행된다. "로컬에서 통과했으니 운영도 통과한다"가 성립하려면 검증 대상 코드가 같아야 한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class S3ImageStorageAdapter implements ImageStoragePort {

    /** S3 DeleteObjects 한 요청의 키 상한. */
    private static final int DELETE_BATCH_SIZE = 1000;

    private static final DateTimeFormatter KEY_DATE = DateTimeFormatter.ofPattern("yyyy/MM");

    /** CDN 뒤의 이미지는 키가 UUID 라 내용이 바뀌지 않는다 — 캐시를 최대로 잡아도 안전하다. */
    private static final String CACHE_CONTROL = "public, max-age=31536000, immutable";

    private final S3Client s3Client;
    private final StorageProperties properties;

    /**
     * {@code {env}/community/{yyyy}/{MM}/{uuid}.{ext}}
     *
     * <p>원본 파일명을 쓰지 않는다 — 경로 조작({@code ../}), 한글·특수문자 인코딩 문제,
     * 파일명으로 인한 정보 노출을 한 번에 없앤다.
     */
    @Override
    public StoredImageLocation allocate(String extension) {
        String key = "%s/community/%s/%s.%s".formatted(
                properties.getKeyPrefix(),
                LocalDate.now().format(KEY_DATE),
                UUID.randomUUID(),
                extension);
        return new StoredImageLocation(key, toUrl(key));
    }

    @Override
    public void store(String storageKey, byte[] content, String contentType) {
        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(properties.getBucket())
                            .key(storageKey)
                            // 저장 시 확정한 값으로 고정한다. 클라이언트가 보낸 헤더를 그대로
                            // 흘리면 브라우저의 MIME 스니핑과 맞물려 XSS 경로가 열린다.
                            .contentType(contentType)
                            .contentDisposition("inline")
                            .cacheControl(CACHE_CONTROL)
                            .build(),
                    RequestBody.fromBytes(content));
        } catch (SdkException e) {
            log.error("S3 업로드 실패: bucket={}, key={}", properties.getBucket(), storageKey, e);
            throw new CoreException(ErrorType.IMAGE_STORAGE_FAILED);
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(storageKey)
                    .build());
        } catch (SdkException e) {
            log.error("S3 삭제 실패: bucket={}, key={}", properties.getBucket(), storageKey, e);
            throw new CoreException(ErrorType.IMAGE_STORAGE_FAILED);
        }
    }

    @Override
    public void deleteAll(List<String> storageKeys) {
        for (int from = 0; from < storageKeys.size(); from += DELETE_BATCH_SIZE) {
            int to = Math.min(from + DELETE_BATCH_SIZE, storageKeys.size());
            deleteChunk(storageKeys.subList(from, to));
        }
    }

    private void deleteChunk(List<String> keys) {
        List<ObjectIdentifier> objects = keys.stream()
                .map(key -> ObjectIdentifier.builder().key(key).build())
                .toList();
        try {
            s3Client.deleteObjects(DeleteObjectsRequest.builder()
                    .bucket(properties.getBucket())
                    .delete(Delete.builder().objects(objects).quiet(true).build())
                    .build());
        } catch (SdkException e) {
            log.error("S3 배치 삭제 실패: bucket={}, count={}", properties.getBucket(), keys.size(), e);
            throw new CoreException(ErrorType.IMAGE_STORAGE_FAILED);
        }
    }

    private String toUrl(String storageKey) {
        String baseUrl = properties.getBaseUrl();
        String normalized = baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;
        return normalized + "/" + storageKey;
    }
}
