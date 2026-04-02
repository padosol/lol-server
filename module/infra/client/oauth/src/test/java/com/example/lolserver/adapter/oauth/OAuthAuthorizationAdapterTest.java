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

    private OAuthProperties.ProviderConfig riotConfig;

    @BeforeEach
    void setUp() {
        riotConfig = new OAuthProperties.ProviderConfig();
        riotConfig.setClientId("riot-client-id");
        riotConfig.setAuthorizationUri(
                "https://auth.riotgames.com/authorize");
        riotConfig.setCallbackUri(
                "http://localhost:8100/api/auth/riot/callback");
        riotConfig.setScope("openid cpid");
    }

    @Test
    @DisplayName("Riot authorization URL 생성")
    void riotAuthorizationUrl() {
        // given
        given(oAuthProperties.getProviderConfig("RIOT")).willReturn(riotConfig);

        // when
        String url = authorizationAdapter.buildAuthorizationUrl(
                OAuthProvider.RIOT, "test-state");

        // then
        assertThat(url).doesNotContain("access_type");
        assertThat(url).contains("client_id=riot-client-id");
        assertThat(url).contains("scope=openid%20cpid");
        assertThat(url).contains("state=test-state");
        assertThat(url).startsWith(
                "https://auth.riotgames.com/authorize");
    }

    @Test
    @DisplayName("Riot callbackUri 반환")
    void getCallbackUri_returnsRiotCallbackUri() {
        // given
        given(oAuthProperties.getProviderConfig("RIOT")).willReturn(riotConfig);

        // when
        String callbackUri = authorizationAdapter.getCallbackUri(
                OAuthProvider.RIOT);

        // then
        assertThat(callbackUri).isEqualTo(
                "http://localhost:8100/api/auth/riot/callback");
    }
}
