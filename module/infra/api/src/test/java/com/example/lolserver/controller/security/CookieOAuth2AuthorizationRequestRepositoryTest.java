package com.example.lolserver.controller.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CookieOAuth2AuthorizationRequestRepository 테스트")
class CookieOAuth2AuthorizationRequestRepositoryTest {

    private CookieOAuth2AuthorizationRequestRepository repository;

    @BeforeEach
    void setUp() {
        repository = new CookieOAuth2AuthorizationRequestRepository();
    }

    @DisplayName("remove 후 같은 request에서 load 시 캐시된 request 반환")
    @Test
    void loadAfterRemove_returnsCachedRequest() {
        // given
        String state = "test-state-123";
        Long linkMemberId = 42L;

        OAuth2AuthorizationRequest authRequest = buildAuthRequest(
                state, Map.of("link_member_id", linkMemberId));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("state", state);
        MockHttpServletResponse response = new MockHttpServletResponse();

        repository.saveAuthorizationRequest(authRequest, request, response);

        // when - Spring Security가 먼저 remove 호출
        repository.removeAuthorizationRequest(request, response);
        // 이후 SuccessHandler에서 load 호출
        OAuth2AuthorizationRequest loaded =
                repository.loadAuthorizationRequest(request);

        // then
        assertThat(loaded).isNotNull();
        assertThat(loaded.getAttributes())
                .containsEntry("link_member_id", linkMemberId);
    }

    @DisplayName("remove 후 다른 request 객체에서 load 시 null 반환")
    @Test
    void loadAfterRemove_differentRequest_returnsNull() {
        // given
        String state = "test-state-456";

        OAuth2AuthorizationRequest authRequest = buildAuthRequest(
                state, Map.of("link_member_id", 1L));

        MockHttpServletRequest requestA = new MockHttpServletRequest();
        requestA.setParameter("state", state);
        MockHttpServletResponse response = new MockHttpServletResponse();

        repository.saveAuthorizationRequest(authRequest, requestA, response);
        repository.removeAuthorizationRequest(requestA, response);

        // when - 다른 request 객체에서 load
        MockHttpServletRequest requestB = new MockHttpServletRequest();
        requestB.setParameter("state", state);
        OAuth2AuthorizationRequest loaded =
                repository.loadAuthorizationRequest(requestB);

        // then
        assertThat(loaded).isNull();
    }

    @DisplayName("remove 전 load 시 store에서 정상 반환")
    @Test
    void load_beforeRemove_returnsFromStore() {
        // given
        String state = "test-state-789";

        OAuth2AuthorizationRequest authRequest = buildAuthRequest(
                state, Map.of());

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("state", state);
        MockHttpServletResponse response = new MockHttpServletResponse();

        repository.saveAuthorizationRequest(authRequest, request, response);

        // when
        OAuth2AuthorizationRequest loaded =
                repository.loadAuthorizationRequest(request);

        // then
        assertThat(loaded).isNotNull();
        assertThat(loaded.getState()).isEqualTo(state);
    }

    @DisplayName("state 파라미터 없으면 null 반환")
    @Test
    void load_noState_returnsNull() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();

        // when
        OAuth2AuthorizationRequest loaded =
                repository.loadAuthorizationRequest(request);

        // then
        assertThat(loaded).isNull();
    }

    private OAuth2AuthorizationRequest buildAuthRequest(
            String state, Map<String, Object> attributes) {
        return OAuth2AuthorizationRequest.authorizationCode()
                .clientId("test-client")
                .authorizationUri("https://example.com/oauth/authorize")
                .redirectUri("https://example.com/callback")
                .state(state)
                .attributes(attrs -> attrs.putAll(attributes))
                .build();
    }
}
