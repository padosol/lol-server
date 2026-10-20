package com.example.lolserver.duo.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * lol-repository 내부 API 호출 설정.
 *
 * <p>{@code internalUrl} 만 환경마다 다르고 나머지는 정책값이라 기본값을 둔다. duo 프로필 조회는
 * 사용자가 글·신청을 만드는 요청 안에서 동기로 일어나므로, lol-repository 가 느려도 그 요청이
 * 오래 묶이지 않게 타임아웃을 짧게 잡고 실패가 이어지면 서킷을 연다.
 */
@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "lol.repository")
public class LolRepositoryClientProperties {

    /**
     * 내부 포트 주소 (prod: {@code http://lol-repository:8081}). 공개 포트는 {@code /internal} 을 404 로 받는다.
     *
     * <p>prod 에서 {@code LOL_REPOSITORY_INTERNAL_URL} 이 빠지면 바인딩은 {@code ${...}} 를 글자 그대로 넣고 기동한다.
     * 그러면 duo 글·신청 생성만 500 으로 깨지므로, http(s) 주소가 아니면 기동에서 막는다.
     */
    @NotBlank
    @Pattern(regexp = "https?://[^\\s${}]+",
            message = "http(s) 주소여야 한다 — prod 는 LOL_REPOSITORY_INTERNAL_URL 환경변수")
    private String internalUrl;

    private Duration connectTimeout = Duration.ofSeconds(1);

    private Duration readTimeout = Duration.ofSeconds(3);

    private CircuitBreaker circuitBreaker = new CircuitBreaker();

    @Getter
    @Setter
    public static class CircuitBreaker {

        /** 최근 몇 번의 호출로 실패율을 계산할지. duo 생성은 드물어서 작게 잡는다. */
        private int slidingWindowSize = 10;

        private int minimumNumberOfCalls = 5;

        private float failureRateThreshold = 50;

        private Duration waitDurationInOpenState = Duration.ofSeconds(30);
    }
}
