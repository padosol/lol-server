package com.example.lolserver.member.adapter.out.oauth;

import com.example.lolserver.member.config.OAuthProperties;
import com.example.lolserver.member.domain.vo.RiotAccount;
import com.example.lolserver.common.error.CoreException;
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
        config.setAccountUri("https://asia.api.riotgames.com/riot/account/v1/accounts/me");
    }

    private void givenAccountApiReturns(String accessToken, Map<String, Object> body) {
        given(oAuthProperties.getProviderConfig("riot")).willReturn(config);
        given(oauthRestClient.get()).willReturn(requestHeadersUriSpec);
        given(requestHeadersUriSpec.uri("https://asia.api.riotgames.com/riot/account/v1/accounts/me"))
                .willReturn(requestHeadersSpec);
        given(requestHeadersSpec.header("Authorization", "Bearer " + accessToken))
                .willReturn(requestHeadersSpec);
        given(requestHeadersSpec.retrieve()).willReturn(responseSpec);
        given(responseSpec.body(Map.class)).willReturn(body);
    }

    @Test
    @DisplayName("fetchAccount로 access token을 사용하여 PUUID와 Riot ID를 조회한다")
    void fetchAccount_success() {
        // given
        givenAccountApiReturns("test-access-token", Map.of(
                "puuid", "fetched-puuid-456",
                "gameName", "Player",
                "tagLine", "KR1"
        ));

        // when
        RiotAccount account = riotRsoClient.fetchAccount("test-access-token");

        // then
        assertThat(account).isEqualTo(new RiotAccount("fetched-puuid-456", "Player", "KR1"));
    }

    @Test
    @DisplayName("fetchAccount에서 Riot ID가 없어도 PUUID만으로 조회된다")
    void fetchAccount_withoutRiotId_returnsPuuidOnly() {
        // given
        givenAccountApiReturns("test-access-token", Map.of("puuid", "fetched-puuid-456"));

        // when
        RiotAccount account = riotRsoClient.fetchAccount("test-access-token");

        // then
        assertThat(account).isEqualTo(new RiotAccount("fetched-puuid-456", null, null));
    }

    @Test
    @DisplayName("fetchAccount에서 응답이 null이면 예외가 발생한다")
    void fetchAccount_nullResponse_throwsException() {
        // given
        givenAccountApiReturns("test-access-token", null);

        // when & then
        assertThatThrownBy(() -> riotRsoClient.fetchAccount("test-access-token"))
                .isInstanceOf(CoreException.class);
    }

    @Test
    @DisplayName("fetchAccount에서 응답에 puuid가 없으면 예외가 발생한다")
    void fetchAccount_missingPuuid_throwsException() {
        // given
        givenAccountApiReturns("test-access-token", Map.of("gameName", "TestPlayer"));

        // when & then
        assertThatThrownBy(() -> riotRsoClient.fetchAccount("test-access-token"))
                .isInstanceOf(CoreException.class);
    }
}
