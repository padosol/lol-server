package com.example.lolserver.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * 오브젝트 스토리지(S3) 클라이언트 빈.
 *
 * <p>{@code RedisConfig}/{@code RestClientConfig} 와 같은 자리다 — 기술 빈 생성만 공유 커널에
 * 두고, 이미지의 생명주기(PENDING→ATTACHED→정리)라는 비즈니스 규칙은 community 컨텍스트가 갖는다.
 *
 * <p><b>프로파일 분기가 없다.</b> 로컬도 운영과 같은 코드 경로로 실제 S3 를 쓰고, 다른 것은
 * 설정값(버킷·키 prefix·CDN 도메인)뿐이다. 로컬만 파일시스템을 타면 S3 권한·키 규칙·CDN 캐시·
 * 삭제 동작이 운영에 배포한 뒤에야 처음 실행되므로, "로컬에서 통과했으니 운영도 통과한다"가
 * 성립하지 않는다. 격리는 코드가 아니라 <b>버킷 분리</b>로 한다(개발자 IAM 정책에 운영 버킷
 * ARN 자체가 등장하지 않는다).
 *
 * <p>{@link DefaultCredentialsProvider} 는 운영에서 ECS Task Role 을, 로컬에서
 * {@code ~/.aws/credentials} 프로필(또는 환경변수)을 같은 체인으로 해석한다. 코드에
 * {@code if (local)} 이 등장하지 않는 이유다.
 *
 * <p>{@code storage.s3.bucket} 이 없는 컨텍스트(테스트 등)에서는 빈을 만들지 않는다.
 * 테스트는 {@code ImageStoragePort} 를 fake 로 대체하므로 실제 클라이언트가 필요 없다.
 */
@Configuration
@ConditionalOnProperty(prefix = "storage.s3", name = "bucket")
public class S3Config {

    @Bean
    public S3Client s3Client(@Value("${storage.s3.region}") String region) {
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}
