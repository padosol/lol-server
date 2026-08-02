package com.example.lolserver.member.adapter.in.web.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OAuth2AuthorizationRequestSerializer 테스트")
class OAuth2AuthorizationRequestSerializerTest {

    private final OAuth2AuthorizationRequestSerializer serializer =
            new OAuth2AuthorizationRequestSerializer();

    @DisplayName("직렬화 후 역직렬화하면 authorization request 필드가 보존된다")
    @Test
    void roundTrip_preservesFields() {
        // given
        OAuth2AuthorizationRequest original = OAuth2AuthorizationRequest
                .authorizationCode()
                .clientId("test-client")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .redirectUri("https://api.example.com/login/oauth2/code/google")
                .scopes(Set.of("openid", "email", "profile"))
                .state("state-123")
                .additionalParameters(params -> params.put("nonce", "nonce-value"))
                .attributes(attrs ->
                        attrs.put(OAuth2ParameterNames.REGISTRATION_ID, "google"))
                .build();

        // when
        OAuth2AuthorizationRequest restored =
                serializer.deserialize(serializer.serialize(original));

        // then
        assertThat(restored).isNotNull();
        assertThat(restored.getState()).isEqualTo("state-123");
        assertThat(restored.getClientId()).isEqualTo("test-client");
        assertThat(restored.getAuthorizationUri())
                .isEqualTo("https://accounts.google.com/o/oauth2/v2/auth");
        assertThat(restored.getRedirectUri())
                .isEqualTo("https://api.example.com/login/oauth2/code/google");
        assertThat(restored.getScopes())
                .containsExactlyInAnyOrder("openid", "email", "profile");
        assertThat(restored.getGrantType())
                .isEqualTo(original.getGrantType());
        assertThat(restored.getResponseType())
                .isEqualTo(original.getResponseType());
        assertThat(restored.getAdditionalParameters())
                .containsEntry("nonce", "nonce-value");
        assertThat(restored.getAttributes())
                .containsEntry(OAuth2ParameterNames.REGISTRATION_ID, "google");
        assertThat(restored.getAuthorizationRequestUri())
                .isEqualTo(original.getAuthorizationRequestUri());
    }

    /**
     * SuccessHandler 는 이 attribute 로 "연동 요청인지" 를 판별하므로, 저장소를 거친 뒤에도
     * 값이 남아 있어야 한다. 값 타입은 default typing 이 붙지 않는 스칼라라 왕복 과정에서
     * 좁은 정수 타입으로 바뀔 수 있어, 저장하는 쪽에서 String 으로 넣는다.
     */
    @DisplayName("연동 요청의 link_member_id attribute 가 왕복 후에도 보존된다")
    @Test
    void roundTrip_preservesLinkMemberId() {
        // given
        OAuth2AuthorizationRequest original = OAuth2AuthorizationRequest
                .authorizationCode()
                .clientId("test-client")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .redirectUri("https://api.example.com/login/oauth2/code/google")
                .state("state-link")
                .attributes(attrs -> attrs.put("link_member_id", "42"))
                .build();

        // when
        OAuth2AuthorizationRequest restored =
                serializer.deserialize(serializer.serialize(original));

        // then
        assertThat(restored).isNotNull();
        assertThat(restored.getAttributes())
                .containsEntry("link_member_id", "42");
    }

    @DisplayName("깨진 payload 는 예외 대신 null 로 흘려보낸다")
    @Test
    void deserialize_brokenPayload_returnsNull() {
        assertThat(serializer.deserialize("{\"not\":\"an auth request\"}"))
                .isNull();
    }
}
