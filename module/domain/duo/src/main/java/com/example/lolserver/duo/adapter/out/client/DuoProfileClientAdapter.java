package com.example.lolserver.duo.adapter.out.client;

import com.example.lolserver.duo.application.port.out.RiotAccountStatsPort;
import com.example.lolserver.duo.domain.vo.RiotAccountStats;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Optional;

/**
 * duo 프로필을 lol-repository 내부 API 로 조회한다 (서비스 분리 설계 5절).
 *
 * <p>404(색인된 적 없는 소환사)는 정상 응답으로 보고 빈 값을 돌려준다 — 서킷 브레이커 실패로도 세지 않는다.
 * 연결 실패·타임아웃·5xx 와 서킷이 열린 상태도 빈 값이다. 듀오 글·신청은 통계 없이 만들어지고, 서킷은
 * lol-repository 가 느릴 때 그 요청들이 타임아웃만큼 묶이지 않게 한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DuoProfileClientAdapter implements RiotAccountStatsPort {

    static final String DUO_PROFILE_URI = "/internal/v1/summoners/{puuid}/duo-profile";

    private final RestClient lolRepositoryRestClient;
    private final CircuitBreaker lolRepositoryCircuitBreaker;

    @Override
    public Optional<RiotAccountStats> findByPuuid(String puuid) {
        try {
            return lolRepositoryCircuitBreaker.executeSupplier(() -> fetch(puuid));
        } catch (RuntimeException e) {
            // 서킷 open(CallNotPermittedException)·연결 실패·5xx·응답 변환 실패 모두 — 서킷은 이미 실패로 기록했다
            log.warn("lol-repository duo-profile 조회 실패 — 통계 없이 진행. puuid={}, cause={}: {}",
                    puuid, e.getClass().getSimpleName(), e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<RiotAccountStats> fetch(String puuid) {
        return lolRepositoryRestClient.get()
                .uri(DUO_PROFILE_URI, puuid)
                .accept(MediaType.APPLICATION_JSON)
                .exchange((request, response) -> {
                    HttpStatusCode status = response.getStatusCode();
                    if (status.isSameCodeAs(HttpStatus.NOT_FOUND)) {
                        return Optional.empty();
                    }
                    if (status.isError()) {
                        throw new RestClientResponseException("duo-profile 응답 " + status.value(),
                                status, response.getStatusText(), response.getHeaders(), null, null);
                    }
                    DuoProfileResponse body = response.bodyTo(DuoProfileResponse.class);
                    if (body == null) {
                        throw new RestClientException("duo-profile 응답 본문이 비어 있다");
                    }
                    return Optional.of(body.toDomain());
                });
    }
}
