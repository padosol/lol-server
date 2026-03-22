package com.example.lolserver.adapter.oauth;

import com.example.lolserver.adapter.oauth.config.OAuthProperties;
import com.example.lolserver.adapter.oauth.dto.OAuthTokenResponse;
import com.example.lolserver.domain.member.domain.vo.OAuthProvider;
import com.example.lolserver.support.error.CoreException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuthTokenExchangerTest {

    @Mock
    private RestClient oauthRestClient;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.RequestBodySpec requestBodySpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private OAuthTokenExchanger tokenExchanger;

    private OAuthProperties.ProviderConfig config;

    @BeforeEach
    void setUp() {
        tokenExchanger = new OAuthTokenExchanger(oauthRestClient);

        config = new OAuthProperties.ProviderConfig();
        config.setClientId("test-client-id");
        config.setClientSecret("test-client-secret");
        config.setTokenUri("https://example.com/token");
    }

    @Test
    @DisplayName("Google 토큰 교환 시 form body에 client_id, client_secret 포함")
    void googleExchange_includesCredentialsInFormBody() {
        // given
        given(oauthRestClient.post()).willReturn(requestBodyUriSpec);
        given(requestBodyUriSpec.uri("https://example.com/token")).willReturn(requestBodySpec);
        given(requestBodySpec.contentType(MediaType.APPLICATION_FORM_URLENCODED)).willReturn(requestBodySpec);
        given(requestBodySpec.body(any(MultiValueMap.class))).willReturn(requestBodySpec);
        given(requestBodySpec.retrieve()).willReturn(responseSpec);
        given(responseSpec.body(Map.class)).willReturn(Map.of(
                "access_token", "google-token",
                "token_type", "Bearer"
        ));

        // when
        OAuthTokenResponse response = tokenExchanger.exchange("code", "redirect", config, OAuthProvider.GOOGLE);

        // then
        assertThat(response.getAccessToken()).isEqualTo("google-token");

        ArgumentCaptor<MultiValueMap<String, String>> bodyCaptor = ArgumentCaptor.forClass(MultiValueMap.class);
        verify(requestBodySpec).body(bodyCaptor.capture());
        MultiValueMap<String, String> body = bodyCaptor.getValue();

        assertThat(body.getFirst("client_id")).isEqualTo("test-client-id");
        assertThat(body.getFirst("client_secret")).isEqualTo("test-client-secret");
        assertThat(body.getFirst("code")).isEqualTo("code");
        assertThat(body.getFirst("grant_type")).isEqualTo("authorization_code");

        verify(requestBodySpec, never()).header(eq("Authorization"), any());
    }

    @Test
    @DisplayName("Riot 토큰 교환 시 Basic Auth 헤더 사용, form body에 credentials 미포함")
    void riotExchange_usesBasicAuthHeader() {
        // given
        String expectedCredentials = Base64.getEncoder()
                .encodeToString("test-client-id:test-client-secret".getBytes(StandardCharsets.UTF_8));

        given(oauthRestClient.post()).willReturn(requestBodyUriSpec);
        given(requestBodyUriSpec.uri("https://example.com/token")).willReturn(requestBodySpec);
        given(requestBodySpec.contentType(MediaType.APPLICATION_FORM_URLENCODED)).willReturn(requestBodySpec);
        given(requestBodySpec.header("Authorization", "Basic " + expectedCredentials)).willReturn(requestBodySpec);
        given(requestBodySpec.body(any(MultiValueMap.class))).willReturn(requestBodySpec);
        given(requestBodySpec.retrieve()).willReturn(responseSpec);
        given(responseSpec.body(Map.class)).willReturn(Map.of(
                "access_token", "riot-token",
                "id_token", "riot-id-token",
                "token_type", "Bearer"
        ));

        // when
        OAuthTokenResponse response = tokenExchanger.exchange("code", "redirect", config, OAuthProvider.RIOT);

        // then
        assertThat(response.getAccessToken()).isEqualTo("riot-token");
        assertThat(response.getIdToken()).isEqualTo("riot-id-token");

        verify(requestBodySpec).header("Authorization", "Basic " + expectedCredentials);

        ArgumentCaptor<MultiValueMap<String, String>> bodyCaptor = ArgumentCaptor.forClass(MultiValueMap.class);
        verify(requestBodySpec).body(bodyCaptor.capture());
        MultiValueMap<String, String> body = bodyCaptor.getValue();

        assertThat(body.containsKey("client_id")).isFalse();
        assertThat(body.containsKey("client_secret")).isFalse();
    }

    @Test
    @DisplayName("토큰 응답에 access_token이 없으면 예외 발생")
    void exchange_throwsWhenNoAccessToken() {
        // given
        given(oauthRestClient.post()).willReturn(requestBodyUriSpec);
        given(requestBodyUriSpec.uri("https://example.com/token")).willReturn(requestBodySpec);
        given(requestBodySpec.contentType(MediaType.APPLICATION_FORM_URLENCODED)).willReturn(requestBodySpec);
        given(requestBodySpec.body(any(MultiValueMap.class))).willReturn(requestBodySpec);
        given(requestBodySpec.retrieve()).willReturn(responseSpec);
        given(responseSpec.body(Map.class)).willReturn(Map.of("error", "invalid_grant"));

        // when & then
        assertThatThrownBy(() -> tokenExchanger.exchange("code", "redirect", config, OAuthProvider.GOOGLE))
                .isInstanceOf(CoreException.class);
    }
}
