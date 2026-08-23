package com.example.lolserver.community.adapter.out.storage;

import com.example.lolserver.common.config.StorageProperties;
import com.example.lolserver.community.application.port.out.StoredImageLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.services.s3.S3Client;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 키 레이아웃 계약을 고정한다.
 *
 * <p>{@code allocate} 는 S3 를 치지 않는다(키·URL 을 PUT 이전에 발급한다) — 그래서 클라이언트는
 * 목으로 충분하다. 여기서 지키는 것은 두 가지 약속이다: 키의 첫 세그먼트가 환경을 가른다는 것
 * (버킷이 하나라 이게 유일한 경계다), 그리고 공개 URL 경로가 S3 키와 정확히 같다는 것.
 * 뒤쪽이 깨지면 업로드는 성공하고 조회만 404 가 되어 원인을 찾기 고약하다.
 */
class S3ImageStorageAdapterTest {

    private final StorageProperties properties = new StorageProperties();
    private S3ImageStorageAdapter adapter;

    @BeforeEach
    void setUp() {
        properties.setBucket("mmrtr-static");
        properties.setRegion("ap-northeast-2");
        properties.setKeyRoot("community-dev");
        properties.setBaseUrl("https://static.metapick.me");
        adapter = new S3ImageStorageAdapter(Mockito.mock(S3Client.class), properties);
    }

    @Test
    @DisplayName("키는 설정된 루트로 시작한다 — CloudFront behavior 의 path pattern 과 맞물린다")
    void 키는_설정된_루트로_시작한다() {
        StoredImageLocation location = adapter.allocate("jpg");

        assertThat(location.storageKey()).startsWith("community-dev/");
    }

    @Test
    @DisplayName("키는 {루트}/{yyyy}/{MM}/{uuid}.{확장자} 형태다")
    void 키_형태() {
        StoredImageLocation location = adapter.allocate("webp");

        assertThat(location.storageKey())
                .matches("community-dev/\\d{4}/\\d{2}/[0-9a-f-]{36}\\.webp");
    }

    @Test
    @DisplayName("공개 URL 경로가 S3 키와 완전히 같다 — 로그의 URI 를 그대로 키로 쓸 수 있다")
    void URL_경로와_키가_같다() {
        StoredImageLocation location = adapter.allocate("png");

        assertThat(location.url())
                .isEqualTo("https://static.metapick.me/" + location.storageKey());
    }

    @Test
    @DisplayName("base-url 끝에 슬래시가 있어도 경로가 겹치지 않는다")
    void base_url_끝_슬래시를_흡수한다() {
        properties.setBaseUrl("https://static.metapick.me/");

        StoredImageLocation location = adapter.allocate("gif");

        assertThat(location.url())
                .isEqualTo("https://static.metapick.me/" + location.storageKey());
    }

    @Test
    @DisplayName("키 루트를 설정하지 않으면 운영 값으로 떨어진다 — null/ 로 나가지 않는다")
    void 키_루트_기본값() {
        StorageProperties defaults = new StorageProperties();
        defaults.setBaseUrl("https://static.metapick.me");
        S3ImageStorageAdapter unconfigured =
                new S3ImageStorageAdapter(Mockito.mock(S3Client.class), defaults);

        StoredImageLocation location = unconfigured.allocate("jpg");

        assertThat(location.storageKey()).startsWith("community/");
    }
}
