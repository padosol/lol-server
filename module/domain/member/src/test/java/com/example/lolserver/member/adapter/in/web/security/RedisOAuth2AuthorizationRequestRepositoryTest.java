package com.example.lolserver.member.adapter.in.web.security;

import com.example.lolserver.member.application.port.out.OAuthAuthorizationRequestPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RedisOAuth2AuthorizationRequestRepository 테스트")
class RedisOAuth2AuthorizationRequestRepositoryTest {

    private FakeAuthorizationRequestStore store;
    private RedisOAuth2AuthorizationRequestRepository repository;

    @BeforeEach
    void setUp() {
        store = new FakeAuthorizationRequestStore();
        repository = newRepository(store);
    }

    @DisplayName("remove 후 같은 request에서 load 시 요청 스코프에 남은 request 반환")
    @Test
    void loadAfterRemove_returnsCachedRequest() {
        // given
        String state = "test-state-123";

        OAuth2AuthorizationRequest authRequest = buildAuthRequest(
                state, Map.of("link_member_id", "42"));

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
                .containsEntry("link_member_id", "42");
    }

    @DisplayName("remove 후 다른 request 객체에서 load 시 null 반환")
    @Test
    void loadAfterRemove_differentRequest_returnsNull() {
        // given
        String state = "test-state-456";

        OAuth2AuthorizationRequest authRequest = buildAuthRequest(
                state, Map.of("link_member_id", "1"));

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

    @DisplayName("remove 전 load 시 저장소에서 정상 반환")
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

    @DisplayName("authorize 요청과 콜백이 다른 인스턴스로 가도 request 를 복원한다")
    @Test
    void savedOnOneInstance_isReadableFromAnother() {
        // given - 같은 Redis 를 보는 별개의 인스턴스
        String state = "test-state-multi";
        RedisOAuth2AuthorizationRequestRepository callbackInstance =
                newRepository(store);

        MockHttpServletRequest authorizeRequest = new MockHttpServletRequest();
        MockHttpServletResponse authorizeResponse = new MockHttpServletResponse();
        repository.saveAuthorizationRequest(
                buildAuthRequest(state, Map.of("link_member_id", "7")),
                authorizeRequest, authorizeResponse);

        // when - 콜백은 다른 인스턴스로 라우팅
        MockHttpServletRequest callbackRequest = new MockHttpServletRequest();
        callbackRequest.setParameter("state", state);
        OAuth2AuthorizationRequest removed =
                callbackInstance.removeAuthorizationRequest(
                        callbackRequest, new MockHttpServletResponse());

        // then
        assertThat(removed).isNotNull();
        assertThat(removed.getState()).isEqualTo(state);
        assertThat(removed.getAttributes())
                .containsEntry("link_member_id", "7");
    }

    @DisplayName("remove 는 저장소에서 state 를 삭제해 재사용을 막는다")
    @Test
    void remove_consumesStateOnce() {
        // given
        String state = "test-state-once";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("state", state);
        MockHttpServletResponse response = new MockHttpServletResponse();

        repository.saveAuthorizationRequest(
                buildAuthRequest(state, Map.of()), request, response);

        // when
        repository.removeAuthorizationRequest(request, response);

        // then - 저장소가 비었고, 새 요청에서는 조회되지 않는다
        assertThat(store.values).isEmpty();
        MockHttpServletRequest replay = new MockHttpServletRequest();
        replay.setParameter("state", state);
        assertThat(repository.removeAuthorizationRequest(
                replay, new MockHttpServletResponse())).isNull();
    }

    @DisplayName("저장 시 5분 TTL 을 건다")
    @Test
    void save_appliesTtl() {
        // given
        String state = "test-state-ttl";
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        repository.saveAuthorizationRequest(
                buildAuthRequest(state, Map.of()), request, response);

        // then
        assertThat(store.ttls).containsEntry(state, 300L);
    }

    @DisplayName("authorizationRequest 가 null 이면 저장하지 않는다")
    @Test
    void save_nullRequest_doesNothing() {
        // when
        repository.saveAuthorizationRequest(null,
                new MockHttpServletRequest(), new MockHttpServletResponse());

        // then
        assertThat(store.values).isEmpty();
    }

    private RedisOAuth2AuthorizationRequestRepository newRepository(
            OAuthAuthorizationRequestPort port) {
        return new RedisOAuth2AuthorizationRequestRepository(
                port, new OAuth2AuthorizationRequestSerializer());
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

    /**
     * 인스턴스 밖에 있는 공유 저장소 역할. 여러 repository 인스턴스가 같은 객체를 보게 해서
     * 다중 인스턴스 배포를 흉내낸다.
     */
    private static class FakeAuthorizationRequestStore
            implements OAuthAuthorizationRequestPort {

        private final Map<String, String> values = new HashMap<>();
        private final Map<String, Long> ttls = new HashMap<>();

        @Override
        public void save(String state, String serializedRequest,
                long ttlSeconds) {
            values.put(state, serializedRequest);
            ttls.put(state, ttlSeconds);
        }

        @Override
        public Optional<String> find(String state) {
            return Optional.ofNullable(values.get(state));
        }

        @Override
        public Optional<String> findAndDelete(String state) {
            ttls.remove(state);
            return Optional.ofNullable(values.remove(state));
        }
    }
}
