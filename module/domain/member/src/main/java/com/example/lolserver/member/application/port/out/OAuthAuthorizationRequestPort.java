package com.example.lolserver.member.application.port.out;

import java.util.Optional;

/**
 * OAuth2 authorization request 를 state 키로 보관하는 저장소.
 *
 * <p>authorize 요청(저장)과 provider 콜백(조회)은 서로 다른 HTTP 요청이고, 다중 인스턴스
 * 환경에서는 다른 인스턴스로 라우팅될 수 있다. 따라서 저장소는 인스턴스 로컬이 아니라
 * 공유 저장소여야 한다.
 *
 * <p>값은 이미 직렬화된 문자열로 오간다 — 직렬화 포맷은 Spring Security 타입을 아는
 * 어댑터 쪽 관심사이고, 이 포트는 "state 키로 TTL 을 걸어 보관/일회성 소비" 라는
 * 저장소 계약만 표현한다.
 */
public interface OAuthAuthorizationRequestPort {

    void save(String state, String serializedRequest, long ttlSeconds);

    Optional<String> find(String state);

    /**
     * state 에 해당하는 값을 조회하면서 동시에 삭제한다 (일회성 소비).
     * state 재사용을 막아야 하므로 조회와 삭제는 원자적으로 수행된다.
     */
    Optional<String> findAndDelete(String state);
}
