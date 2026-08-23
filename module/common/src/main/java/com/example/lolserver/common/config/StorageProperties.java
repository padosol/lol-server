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
 *   keyRoot    community-dev                  community
 *   baseUrl    static.metapick.me             static.metapick.me
 * </pre>
 *
 * <p>격리는 버킷이 한다 — 개발자 IAM 정책에 운영 버킷 ARN 자체가 등장하지 않으므로,
 * 정책 실수의 여지가 "존재하지 않는 권한"으로 바뀐다. 도메인은 두 환경이 공유하고
 * ({@code static.metapick.me}) 경로로 갈린다 — CloudFront 는 Host 가 아니라 <b>경로</b>로
 * 라우팅하므로, 한 배포로 두 버킷을 쓰려면 키의 첫 세그먼트가 갈리는 수밖에 없다.
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

    /**
     * 키의 첫 세그먼트. <b>CloudFront cache behavior 의 path pattern 과 반드시 같아야 한다.</b>
     * 한 배포가 {@code /community/*} 는 운영 버킷으로, {@code /community-dev/*} 는 dev 버킷으로
     * 보내므로 둘이 어긋나면 업로드는 성공하는데 조회만 404 가 된다 — 원인을 찾기 고약한 형태다.
     *
     * <p>기본값을 둔 이유는 값이 빠졌을 때 키가 {@code null/…} 로 나가는 것을 막기 위해서다.
     */
    private String keyRoot = "community";

    /** CloudFront 배포 도메인. 버킷은 비공개이고 OAC 로만 읽힌다. */
    private String baseUrl;
}
