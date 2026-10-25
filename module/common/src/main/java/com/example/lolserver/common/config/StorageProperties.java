package com.example.lolserver.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 오브젝트 스토리지 설정. 로컬과 운영이 다른 것은 <b>{@code keyRoot} 하나뿐</b>이다 —
 * 어댑터도, 프로파일 분기도, 버킷도 하나다.
 *
 * <pre>
 *              local                          prod
 *   bucket     mmrtr-static                   mmrtr-static
 *   keyRoot    community-dev                  community
 *   baseUrl    static.metapick.me             static.metapick.me
 * </pre>
 *
 * <p>버킷을 게임 정적 자산과 함께 쓰는 이유는 CloudFront 가 Host 가 아니라 <b>경로</b>로
 * 라우팅하기 때문이다. {@code static.metapick.me} 배포는 오리진이 이 버킷 하나이고 기본
 * behavior 가 전 경로를 받으므로, 키를 여기에 넣는 순간 배포 설정을 건드리지 않고 그대로
 * 서빙된다. 버킷을 나누면 경로마다 오리진과 behavior 를 새로 얹어야 한다.
 *
 * <p>대신 환경 격리는 버킷이 아니라 <b>키 prefix</b> 가 진다. 개발자 IAM 의 리소스를
 * {@code arn:aws:s3:::mmrtr-static/community-dev/*} 로 좁혀 두어야 격리가 실제로 성립한다 —
 * 버킷 단위로 열어 두면 로컬 크리덴셜이 운영 이미지는 물론 게임 정적 자산까지 지울 수 있다.
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
     * 키의 첫 세그먼트. 버킷도 도메인도 배포도 모두 같으므로 <b>환경을 가르는 값은 이것뿐</b>이다 —
     * 운영 {@code community}, 로컬 {@code community-dev}. 로컬에서 이 값이 운영 것으로 새면
     * 로컬 업로드가 운영 이미지 경로에 그대로 쓰인다. 그래서 IAM 리소스도 같은 prefix 로 좁힌다.
     *
     * <p>기본값을 둔 이유는 값이 빠졌을 때 키가 {@code null/…} 로 나가는 것을 막기 위해서다.
     */
    private String keyRoot = "community";

    /** CloudFront 배포 도메인. 버킷은 비공개이고 OAC 로만 읽힌다. */
    private String baseUrl;
}
