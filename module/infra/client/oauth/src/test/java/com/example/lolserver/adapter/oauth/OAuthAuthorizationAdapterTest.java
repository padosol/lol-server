package com.example.lolserver.adapter.oauth;

import com.example.lolserver.adapter.oauth.config.OAuthProperties;
import com.example.lolserver.domain.member.domain.vo.OAuthProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class OAuthAuthorizationAdapterTest {

    @Mock
    private OAuthProperties oAuthProperties;

    @InjectMocks
    private OAuthAuthorizationAdapter authorizationAdapter;

    private OAuthProperties.ProviderConfig googleConfig;
    private OAuthProperties.ProviderConfig riotConfig;

    @BeforeEach
    void setUp() {
        googleConfig = new OAuthProperties.ProviderConfig();
        googleConfig.setClientId("google-client-id");
        googleConfig.setAuthorizationUri("https://accounts.google.com/o/oauth2/v2/auth");
        googleConfig.setCallbackUri("http://localhost:8100/api/auth/google/callback");
        googleConfig.setScope("openid email profile");

        riotConfig = new OAuthProperties.ProviderConfig();
        riotConfig.setClientId("riot-client-id");
        riotConfig.setAuthorizationUri("https://auth.riotgames.com/authorize");
        riotConfig.setCallbackUri("http://localhost:8100/api/auth/riot/callback");
        riotConfig.setScope("openid cpid");
    }

    @Test
    @DisplayName("Google authorization URL에 access_type=offline 포함")
    void googleAuthorizationUrl_includesAccessTypeOffline() {
        // given
        given(oAuthProperties.getGoogle()).willReturn(googleConfig);

        // when
        String url = authorizationAdapter.buildAuthorizationUrl(OAuthProvider.GOOGLE, "test-state");

        // then
        assertThat(url).contains("access_type=offline");
        assertThat(url).contains("client_id=google-client-id");
        assertThat(url).contains("state=test-state");
        assertThat(url).contains("response_type=code");
        assertThat(url).startsWith("https://accounts.google.com/o/oauth2/v2/auth");
    }

    @Test
    @DisplayName("Riot authorization URL에 access_type=offline 미포함")
    void riotAuthorizationUrl_doesNotIncludeAccessTypeOffline() {
        // given
        given(oAuthProperties.getRiot()).willReturn(riotConfig);

        // when
        String url = authorizationAdapter.buildAuthorizationUrl(OAuthProvider.RIOT, "test-state");

        // then
        assertThat(url).doesNotContain("access_type");
        assertThat(url).contains("client_id=riot-client-id");
        assertThat(url).contains("scope=openid%20cpid");
        assertThat(url).contains("state=test-state");
        assertThat(url).startsWith("https://auth.riotgames.com/authorize");
    }

    @Test
    @DisplayName("Riot callbackUri 반환")
    void getCallbackUri_returnsRiotCallbackUri() {
        // given
        given(oAuthProperties.getRiot()).willReturn(riotConfig);

        // when
        String callbackUri = authorizationAdapter.getCallbackUri(OAuthProvider.RIOT);

        // then
        assertThat(callbackUri).isEqualTo("http://localhost:8100/api/auth/riot/callback");
    }
}
