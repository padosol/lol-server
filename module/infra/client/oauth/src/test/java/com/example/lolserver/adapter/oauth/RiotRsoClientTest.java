package com.example.lolserver.adapter.oauth;

import com.example.lolserver.adapter.oauth.config.OAuthProperties;
import com.example.lolserver.adapter.oauth.dto.OAuthTokenResponse;
import com.example.lolserver.domain.member.application.model.OAuthUserInfo;
import com.example.lolserver.domain.member.domain.vo.OAuthProvider;
import com.example.lolserver.support.error.CoreException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class RiotRsoClientTest {

    @Mock
    private RestClient oauthRestClient;

    @Mock
    private OAuthProperties oAuthProperties;

    @Mock
    private OAuthTokenExchanger tokenExchanger;

    @Mock
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private RestClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    @InjectMocks
    private RiotRsoClient riotRsoClient;

    private OAuthProperties.ProviderConfig config;

    @BeforeEach
    void setUp() {
        config = new OAuthProperties.ProviderConfig();
        config.setClientId("riot-client-id");
        config.setClientSecret("riot-client-secret");
        config.setTokenUri("https://auth.riotgames.com/token");
        config.setUserInfoUri("https://auth.riotgames.com/userinfo");
        config.setAccountUri("https://americas.api.riotgames.com/riot/account/v1/accounts/me");
    }

    @Test
    @DisplayName("RSO userinfo와 Account API를 순차 호출하여 OAuthUserInfo 생성")
    void getUserInfo_callsUserInfoAndAccountApi() {
        // given
        given(oAuthProperties.getProviderConfig("riot")).willReturn(config);
        given(tokenExchanger.exchange("code", "redirect", config, OAuthProvider.RIOT))
                .willReturn(OAuthTokenResponse.builder()
                        .accessToken("riot-access-token")
                        .idToken("riot-id-token")
                        .build());

        // userinfo 호출
        given(oauthRestClient.get()).willReturn(requestHeadersUriSpec);
        given(requestHeadersUriSpec.uri("https://auth.riotgames.com/userinfo")).willReturn(requestHeadersSpec);
        given(requestHeadersSpec.header("Authorization", "Bearer riot-access-token")).willReturn(requestHeadersSpec);
        given(requestHeadersSpec.retrieve()).willReturn(responseSpec);
        given(responseSpec.body(Map.class)).willReturn(Map.of("sub", "rso-sub-12345"));

        // account API 호출 - 두 번째 get() 호출을 위한 새로운 mock chain
        RestClient.RequestHeadersUriSpec accountUriSpec = org.mockito.Mockito.mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec accountHeadersSpec = org.mockito.Mockito.mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec accountResponseSpec = org.mockito.Mockito.mock(RestClient.ResponseSpec.class);

        given(oauthRestClient.get())
                .willReturn(requestHeadersUriSpec)    // 첫 번째: userinfo
                .willReturn(accountUriSpec);           // 두 번째: account
        given(accountUriSpec.uri("https://americas.api.riotgames.com/riot/account/v1/accounts/me"))
                .willReturn(accountHeadersSpec);
        given(accountHeadersSpec.header("Authorization", "Bearer riot-access-token"))
                .willReturn(accountHeadersSpec);
        given(accountHeadersSpec.retrieve()).willReturn(accountResponseSpec);
        given(accountResponseSpec.body(Map.class)).willReturn(Map.of(
                "puuid", "test-puuid-123",
                "gameName", "TestPlayer",
                "tagLine", "KR1"
        ));

        // when
        OAuthUserInfo userInfo = riotRsoClient.getUserInfo("code", "redirect");

        // then
        assertThat(userInfo.getProvider()).isEqualTo("RIOT");
        assertThat(userInfo.getProviderId()).isEqualTo("rso-sub-12345");
        assertThat(userInfo.getPuuid()).isEqualTo("test-puuid-123");
        assertThat(userInfo.getGameName()).isEqualTo("TestPlayer");
        assertThat(userInfo.getTagLine()).isEqualTo("KR1");
    }

    @Test
    @DisplayName("userinfo에서 sub가 null이면 예외 발생")
    void getUserInfo_throwsWhenSubIsNull() {
        // given
        given(oAuthProperties.getProviderConfig("riot")).willReturn(config);
        given(tokenExchanger.exchange("code", "redirect", config, OAuthProvider.RIOT))
                .willReturn(OAuthTokenResponse.builder()
                        .accessToken("riot-access-token")
                        .build());

        given(oauthRestClient.get()).willReturn(requestHeadersUriSpec);
        given(requestHeadersUriSpec.uri("https://auth.riotgames.com/userinfo")).willReturn(requestHeadersSpec);
        given(requestHeadersSpec.header("Authorization", "Bearer riot-access-token")).willReturn(requestHeadersSpec);
        given(requestHeadersSpec.retrieve()).willReturn(responseSpec);
        given(responseSpec.body(Map.class)).willReturn(Map.of("cpid", "some-cpid"));

        // when & then
        assertThatThrownBy(() -> riotRsoClient.getUserInfo("code", "redirect"))
                .isInstanceOf(CoreException.class);
    }
}
