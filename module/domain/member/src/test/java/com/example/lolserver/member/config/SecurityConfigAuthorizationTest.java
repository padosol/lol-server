package com.example.lolserver.member.config;

import com.example.lolserver.member.adapter.in.web.security.AuthCookieManager;
import com.example.lolserver.member.adapter.in.web.security.JwtAuthenticationFilter;
import com.example.lolserver.member.adapter.in.web.security.JwtTokenAdapter;
import com.example.lolserver.member.adapter.in.web.security.OAuth2AuthenticationFailureHandler;
import com.example.lolserver.member.adapter.in.web.security.OAuth2AuthenticationSuccessHandler;
import com.example.lolserver.member.adapter.in.web.security.RedisOAuth2AuthorizationRequestRepository;
import com.example.lolserver.member.adapter.in.web.security.RestAuthenticationEntryPoint;
import com.example.lolserver.member.adapter.in.web.security.SocialAccountLinkTokenStore;
import com.example.lolserver.member.adapter.in.web.security.oauth2.CustomOidcUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 실제 {@link SecurityConfig} 필터 체인으로 경로별 인가 규칙을 검증한다.
 * 컨트롤러는 모든 경로에 200 을 주는 더미라서, 응답 코드는 오직 인가 결과만 나타낸다.
 */
@DisplayName("SecurityConfig 경로별 인가 규칙")
@SpringJUnitWebConfig(SecurityConfigAuthorizationTest.TestConfig.class)
class SecurityConfigAuthorizationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtTokenAdapter jwtTokenAdapter;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @DisplayName("비로그인 사용자에게 공개된 경로 — 새 경로·옛 경로 모두")
    @ParameterizedTest(name = "{0} {1}")
    @CsvSource({
        // Riot 데이터 — 새 경로
        "GET, /api/v1/summoners/by-riot-id/hide-KR1",
        "GET, /api/v1/summoners/autocomplete",
        "GET, /api/v1/summoners/puuid-1",
        "GET, /api/v1/summoners/puuid-1/renewal",
        "POST, /api/v1/summoners/puuid-1/renewal",
        "GET, /api/v1/summoners/puuid-1/leagues",
        "GET, /api/v1/summoners/puuid-1/active-game",
        "GET, /api/v1/summoners/puuid-1/matches",
        "GET, /api/v1/summoners/puuid-1/champions",
        "GET, /api/v1/matches/KR_1",
        "GET, /api/v1/matches/KR_1/timeline",
        "GET, /api/v1/rankings",
        "GET, /api/v1/rankings/tier-cutoffs",
        "GET, /api/v1/champion-stats",
        "GET, /api/v1/champion-stats/266",
        "GET, /api/v1/game-data/versions/latest",
        "GET, /api/v1/game-data/queues",
        "GET, /api/v1/game-data/champion-rotations",
        // Riot 데이터 — 옛 경로
        "GET, /api/v1/summoners/kr/hide-KR1",
        "GET, /api/v1/kr/summoners/puuid-1",
        "GET, /api/v1/kr/summoners/puuid-1/renewal",
        "GET, /api/v1/summoners/puuid-1/renewal-status",
        "GET, /api/v1/kr/summoners/puuid-1/matches",
        "GET, /api/v1/kr/matches/matchIds",
        "GET, /api/v1/kr/matches",
        "GET, /api/v1/rank/champions",
        "GET, /api/v1/match/timeline/KR_1",
        "GET, /api/v1/leagues/by-puuid/puuid-1",
        "GET, /api/v1/kr/spectator/active-games/by-puuid/puuid-1",
        "GET, /api/v1/kr/rank",
        "GET, /api/v1/kr/tier-cutoffs",
        "GET, /api/v1/kr/tier-cutoffs/RANKED_SOLO_5x5/CHALLENGER",
        "GET, /api/v1/kr/champion-stats",
        "GET, /api/v1/kr/champion-stats/positions",
        "GET, /api/v1/kr/champion/rotation",
        "GET, /api/v1/versions",
        "GET, /api/v1/seasons/1",
        "GET, /api/v1/patch-notes",
        "GET, /api/v1/queue-tab",
        // 인증 — refresh 는 액세스 토큰 없이 호출된다
        "POST, /api/v1/auth/refresh",
        "POST, /api/auth/refresh",
        // 커뮤니티·듀오 조회
        "GET, /api/v1/community/posts",
        "GET, /api/v1/community/posts/1",
        "GET, /api/v1/community/posts/1/comments",
        "GET, /api/v1/community/categories",
        "GET, /api/community/posts",
        "GET, /api/community/posts/search",
        "GET, /api/community/categories",
        "GET, /api/v1/duo/posts",
        "GET, /api/v1/duo/posts/1",
        "GET, /api/duo/posts",
    })
    void 비로그인_공개(String method, String path) throws Exception {
        mockMvc.perform(request(HttpMethod.valueOf(method), path))
                .andExpect(status().isOk());
    }

    @DisplayName("로그인이 필요한 경로 — 비로그인이면 401")
    @ParameterizedTest(name = "{0} {1}")
    @CsvSource({
        // 회원·인증
        "GET, /api/v1/members/me",
        "PATCH, /api/v1/members/me",
        "DELETE, /api/v1/members/me",
        "GET, /api/v1/members/me/social-accounts/link/google",
        "GET, /api/members/me",
        "PATCH, /api/members/me/nickname",
        "POST, /api/v1/auth/logout",
        "POST, /api/auth/logout",
        // 커뮤니티 변경·개인화
        "POST, /api/v1/community/posts",
        "PUT, /api/v1/community/posts/1",
        "DELETE, /api/v1/community/posts/1",
        "PUT, /api/v1/community/posts/1/bookmark",
        "DELETE, /api/v1/community/posts/1/bookmark",
        "PUT, /api/v1/community/posts/1/vote",
        "PUT, /api/v1/community/comments/1/vote",
        "POST, /api/v1/community/posts/1/comments",
        "GET, /api/v1/community/me/posts",
        "GET, /api/v1/community/me/bookmarks",
        "POST, /api/v1/community/images",
        "POST, /api/community/bookmarks",
        "POST, /api/community/votes",
        "GET, /api/community/me/posts",
        // 듀오 변경·개인화
        "POST, /api/v1/duo/posts",
        "GET, /api/v1/duo/posts/1/requests",
        "GET, /api/v1/duo/posts/1/match-result",
        "POST, /api/v1/duo/requests/1/accept",
        "POST, /api/v1/duo/requests/1/cancel",
        "GET, /api/v1/duo/me/requests",
        "GET, /api/v1/duo/notifications/stream",
        "GET, /api/duo/posts/1/requests",
        "PUT, /api/duo/requests/1/accept",
        "GET, /api/duo/notifications/subscribe",
        // Riot 데이터는 조회와 갱신 요청만 공개
        "POST, /api/v1/summoners/puuid-1",
        "DELETE, /api/v1/game-data/versions/1",
        // 정의되지 않은 리소스
        "GET, /api/v1/unknown",
    })
    void 비로그인_401(String method, String path) throws Exception {
        mockMvc.perform(request(HttpMethod.valueOf(method), path))
                .andExpect(status().isUnauthorized());
    }

    @DisplayName("로그인 사용자는 회원·커뮤니티·듀오 새 경로에 접근할 수 있다")
    @ParameterizedTest(name = "{0} {1}")
    @CsvSource({
        "GET, /api/v1/members/me",
        "PATCH, /api/v1/members/me",
        "POST, /api/v1/auth/logout",
        "PUT, /api/v1/community/posts/1/bookmark",
        "PUT, /api/v1/community/posts/1/vote",
        "GET, /api/v1/community/me/bookmarks",
        "POST, /api/v1/duo/requests/1/accept",
        "GET, /api/v1/duo/notifications/stream",
        "GET, /api/members/me",
    })
    void 로그인_200(String method, String path) throws Exception {
        mockMvc.perform(request(HttpMethod.valueOf(method), path)
                        .header("Authorization", "Bearer " + accessToken("USER")))
                .andExpect(status().isOk());
    }

    @DisplayName("어드민 경로는 ADMIN 권한이 없으면 403")
    @Test
    void 어드민_403() throws Exception {
        mockMvc.perform(request(HttpMethod.GET, "/api/v1/admin/anything")
                        .header("Authorization", "Bearer " + accessToken("USER")))
                .andExpect(status().isForbidden());
    }

    private String accessToken(String role) {
        return jwtTokenAdapter.generateAccessToken(1L, role);
    }

    @Configuration
    @EnableWebMvc
    @Import(SecurityConfig.class)
    static class TestConfig {

        @Bean
        JwtTokenAdapter jwtTokenAdapter() {
            JwtProperties jwtProperties = new JwtProperties();
            jwtProperties.setSecretKey(
                    "test-secret-key-must-be-at-least-256-bits-long-for-hs256-algorithm");
            jwtProperties.setAccessTokenExpiry(1800);
            jwtProperties.setRefreshTokenExpiry(1209600);
            JwtTokenAdapter adapter = new JwtTokenAdapter(jwtProperties);
            adapter.init();
            return adapter;
        }

        @Bean
        JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenAdapter jwtTokenAdapter) {
            return new JwtAuthenticationFilter(jwtTokenAdapter,
                    new AuthCookieManager(new JwtProperties(), new CookieProperties()));
        }

        @Bean
        CorsProperties corsProperties() {
            CorsProperties corsProperties = new CorsProperties();
            corsProperties.setAllowedOrigins(List.of("http://localhost:3000"));
            return corsProperties;
        }

        @Bean
        OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler() {
            return mock(OAuth2AuthenticationSuccessHandler.class);
        }

        @Bean
        OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler() {
            return mock(OAuth2AuthenticationFailureHandler.class);
        }

        @Bean
        RedisOAuth2AuthorizationRequestRepository redisOAuth2AuthorizationRequestRepository() {
            return mock(RedisOAuth2AuthorizationRequestRepository.class);
        }

        @Bean
        CustomOidcUserService customOidcUserService() {
            return mock(CustomOidcUserService.class);
        }

        @Bean
        ClientRegistrationRepository clientRegistrationRepository() {
            return mock(ClientRegistrationRepository.class);
        }

        @Bean
        SocialAccountLinkTokenStore socialAccountLinkTokenStore() {
            return new SocialAccountLinkTokenStore();
        }

        @Bean
        RestAuthenticationEntryPoint restAuthenticationEntryPoint() {
            return new RestAuthenticationEntryPoint(new ObjectMapper());
        }

        @Bean
        AnyPathController anyPathController() {
            return new AnyPathController();
        }
    }

    @RestController
    static class AnyPathController {

        @RequestMapping("/**")
        String ok() {
            return "ok";
        }
    }
}
