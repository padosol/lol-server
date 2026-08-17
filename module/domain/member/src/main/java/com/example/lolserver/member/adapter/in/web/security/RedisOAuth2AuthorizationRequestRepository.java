package com.example.lolserver.member.adapter.in.web.security;

import com.example.lolserver.member.application.port.out.OAuthAuthorizationRequestPort;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

/**
 * 진행 중인 OAuth2 authorization request 를 Redis 에 보관한다.
 *
 * <p>authorize 요청과 provider 콜백은 별개의 HTTP 요청이라, 저장소가 인스턴스 로컬이면
 * 두 요청이 다른 인스턴스로 라우팅되는 순간 state 조회가 실패한다. 공유 저장소를 써서
 * 인스턴스를 늘려도 로그인이 깨지지 않게 한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    private static final int EXPIRE_SECONDS = 300;
    private static final String REMOVED_AUTH_REQUEST_ATTR =
            RedisOAuth2AuthorizationRequestRepository.class.getName()
                    + ".REMOVED_REQUEST";

    private final OAuthAuthorizationRequestPort authorizationRequestPort;
    private final OAuth2AuthorizationRequestSerializer serializer;

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(
            HttpServletRequest request) {
        String state = request.getParameter("state");
        if (state == null) {
            return null;
        }

        OAuth2AuthorizationRequest stored = authorizationRequestPort.find(state)
                .map(serializer::deserialize)
                .orElse(null);
        if (stored != null) {
            return stored;
        }

        // Spring Security 가 인증 처리 중 이미 remove 한 뒤, 같은 요청의
        // SuccessHandler 가 link_member_id 를 읽으려고 다시 load 하는 경로.
        // 저장소에는 없으므로 remove 시 요청 스코프에 남겨둔 값으로 돌려준다.
        Object removed = request.getAttribute(REMOVED_AUTH_REQUEST_ATTR);
        if (removed instanceof OAuth2AuthorizationRequest removedRequest) {
            return removedRequest;
        }
        return null;
    }

    @Override
    public void saveAuthorizationRequest(
            OAuth2AuthorizationRequest authorizationRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        if (authorizationRequest == null) {
            return;
        }

        String state = authorizationRequest.getState();
        authorizationRequestPort.save(state,
                serializer.serialize(authorizationRequest), EXPIRE_SECONDS);

        log.debug("[OAuth2 State] 저장 - state: {}, redirectUri: {}",
                state, authorizationRequest.getRedirectUri());
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(
            HttpServletRequest request,
            HttpServletResponse response) {
        String state = request.getParameter("state");
        if (state == null) {
            return null;
        }

        OAuth2AuthorizationRequest authRequest =
                authorizationRequestPort.findAndDelete(state)
                        .map(serializer::deserialize)
                        .orElse(null);
        if (authRequest == null) {
            log.debug("[OAuth2 State] 조회 실패 - state: {}", state);
            return null;
        }

        log.debug("[OAuth2 State] 조회 성공 - state: {}", state);
        request.setAttribute(REMOVED_AUTH_REQUEST_ATTR, authRequest);
        return authRequest;
    }
}
