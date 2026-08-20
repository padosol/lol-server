package com.example.lolserver.community.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 커뮤니티 이미지 정책값. 코드에 상수로 박지 않는 이유는 운영에서 조정할 여지를 남기기 위해서다
 * (특히 용량·개수 상한과 정리 유예).
 *
 * <p>필드 기본값을 두어 yml 이 없어도 부팅된다 — 이 값들은 운영 비밀이 아니라 정책이고,
 * 빠졌을 때 부팅을 실패시켜야 하는 것은 {@code storage.s3.*}(어느 버킷을 쓸지) 쪽이다.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "community.image")
public class CommunityImageProperties {

    /** 앱 레벨 용량 상한. {@code spring.servlet.multipart.max-file-size} 와 이중 방어다. */
    private long maxSizeBytes = 5 * 1024 * 1024L;

    private int maxCountPerPost = 10;

    /**
     * 픽셀 수 상한(폭탄 이미지 방어). 압축률이 높은 이미지는 파일 크기가 작아도
     * 디코드하는 순간 수 GB 의 힙을 요구할 수 있어, 용량 검사만으로는 막을 수 없다.
     */
    private long maxPixels = 50_000_000L;

    /** 이 폭을 넘으면 비율을 유지해 축소한다. 4000px 사진이 목록에 그대로 내려가지 않도록. */
    private int maxWidth = 1920;

    /** SVG 는 의도적으로 없다 — 스크립트를 담을 수 있어 저장형 XSS 가 된다. */
    private List<String> allowedTypes =
            List.of("image/jpeg", "image/png", "image/gif", "image/webp");

    private int uploadRatePerMinute = 10;

    /** S3 PUT 실패·앱 크래시로 UPLOADING 에 남은 행의 유예. */
    private int uploadingRetentionHours = 1;

    /** 글을 쓰다 이탈해 PENDING 에 남은 행의 유예. 본문에는 URL 이 있는데 imageIds 에서 빠진
     * 경우를 감안해 넉넉히 둔다. */
    private int pendingRetentionHours = 24;

    /** 글 삭제·수정으로 떨어져 나온 행의 유예. 복구 요청과 CDN 잔여 참조를 감안한 값. */
    private int detachedRetentionDays = 7;

    /** 정리 배치가 한 번의 패스에서 처리할 최대 건수. */
    private int cleanupBatchSize = 200;
}
