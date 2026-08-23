package com.example.lolserver.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 오브젝트 스토리지 설정. 로컬과 운영이 다른 것은 <b>이 값들뿐</b>이다 —
 * 어댑터도, 프로파일 분기도 하나다.
 *
 * <pre>
 *              local                          prod
 *   bucket     mmrtr-community-dev            mmrtr-community
 *   baseUrl    dev CloudFront                 prod CloudFront
 * </pre>
 *
 * <p>격리는 버킷이 한다 — 개발자 IAM 정책에 운영 버킷 ARN 자체가 등장하지 않으므로,
 * 정책 실수의 여지가 "존재하지 않는 권한"으로 바뀐다. 키에는 환경을 넣지 않는다:
 * 버킷과 CDN 도메인이 이미 환경을 가르고, 키에 넣으면 공개 URL 에 {@code prod/} 가
 * 그대로 드러나면서 아무것도 더 막지 못한다.
 *
 * <p>{@code S3Config}(빈 생성)와 {@code S3ImageStorageAdapter}(키 조립·PUT/DELETE)가 함께
 * 쓰므로 공유 커널에 둔다. 값을 쓰는 쪽이 두 모듈에 걸쳐 있어 어느 한쪽 컨텍스트로 내리면
 * 나머지가 참조할 수 없다.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "storage.s3")
public class StorageProperties {

    private String bucket;

    private String region;

    /** CloudFront 배포 도메인. 버킷은 비공개이고 OAC 로만 읽힌다. */
    private String baseUrl;
}
