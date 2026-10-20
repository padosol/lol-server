package com.example.lolserver.duo.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

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
@Component
@ConfigurationProperties(prefix = "lol.repository")
public class LolRepositoryClientProperties {

    /** 내부 포트 주소 (prod: {@code http://lol-repository:8081}). 공개 포트는 {@code /internal} 을 404 로 받는다. */
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
