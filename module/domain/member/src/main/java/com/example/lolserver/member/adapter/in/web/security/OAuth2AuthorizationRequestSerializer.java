package com.example.lolserver.member.adapter.in.web.security;

import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

/**
 * {@link OAuth2AuthorizationRequest} 를 공유 저장소에 넣을 수 있는 JSON 문자열로 변환한다.
 *
 * <p>필드를 직접 나열해 DTO 로 옮기지 않고 Spring Security 가 제공하는 Jackson 모듈
 * (mixin) 을 쓴다. PKCE 의 {@code code_verifier} 나 OIDC 의 {@code nonce} 처럼
 * attributes/additionalParameters 에만 실려 오는 값들이 있어서, 직접 매핑하면 설정을
 * 바꾸는 순간 조용히 누락된다.
 *
 * <p>{@code SecurityJackson2Modules} 는 allowlist 기반 default typing 을 켜서
 * {@code @class} 타입 정보를 함께 기록한다. 즉 이 JSON 은 Spring Security 클래스명에
 * 묶여 있다 — 버전 업으로 관련 클래스가 이동/리네임되면 기존 엔트리는 역직렬화에
 * 실패한다. 다만 authorization request 는 TTL 5분짜리 진행 중 로그인 상태이므로,
 * 배포 시점에 깨지는 엔트리는 그 순간 로그인 중이던 요청뿐이고 재시도로 복구된다.
 */
@Slf4j
@Component
public class OAuth2AuthorizationRequestSerializer {

    private final ObjectMapper objectMapper;

    public OAuth2AuthorizationRequestSerializer() {
        this.objectMapper = JsonMapper.builder()
                .addModules(SecurityJackson2Modules.getModules(
                        OAuth2AuthorizationRequestSerializer.class
                                .getClassLoader()))
                .build();
    }

    public String serialize(OAuth2AuthorizationRequest authorizationRequest) {
        try {
            return objectMapper.writeValueAsString(authorizationRequest);
        } catch (Exception e) {
            log.error("OAuth2 authorization request 직렬화 실패", e);
            throw new CoreException(ErrorType.OAUTH_LOGIN_FAILED,
                    "OAuth2 authorization request 직렬화에 실패했습니다.");
        }
    }

    /**
     * 역직렬화 실패는 예외로 올리지 않고 {@code null} 을 반환한다.
     * 호출부({@link RedisOAuth2AuthorizationRequestRepository})는 Spring Security 의
     * {@code AuthorizationRequestRepository} 계약상 "없음" 을 null 로 표현해야 하고,
     * 깨진 엔트리는 state 불일치와 똑같이 인증 실패로 흘려보내는 편이 안전하다.
     */
    public OAuth2AuthorizationRequest deserialize(String json) {
        try {
            return objectMapper.readValue(json, OAuth2AuthorizationRequest.class);
        } catch (Exception e) {
            log.warn("OAuth2 authorization request 역직렬화 실패: {}", e.getMessage());
            return null;
        }
    }
}
