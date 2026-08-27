package com.example.lolserver.community.application.port.out;

import java.util.List;

/**
 * 오브젝트 스토리지 추상.
 *
 * <p>바이트만 주고받으므로 애플리케이션 계층에 {@code MultipartFile}·{@code S3Client} 가
 * 들어오지 않는다(ArchUnit 이 강제). 트래픽이 커져 presigned PUT 으로 이행하게 되면
 * 이 인터페이스에 {@code createUploadTicket()} 을 추가하고 어댑터만 바꾸면 된다.
 */
public interface ImageStoragePort {

    /**
     * 키와 최종 URL 을 미리 발급한다. 저장은 하지 않는다.
     *
     * <p>업로드 파이프라인이 "DB INSERT → S3 PUT → DB UPDATE" 순서이기 때문에 필요한 메서드다.
     * PUT 이 키를 정하는 구조였다면 INSERT 시점에 키를 알 수 없다.
     */
    StoredImageLocation allocate(String extension);

    void store(String storageKey, byte[] content, String contentType);

    void delete(String storageKey);

    /** 정리 배치용 배치 삭제. 이미 없는 객체에 대한 삭제는 멱등이므로 재시도가 안전하다. */
    void deleteAll(List<String> storageKeys);
}
