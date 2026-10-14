package com.example.lolserver.member.config;

import com.example.lolserver.member.adapter.in.web.security.RedisOAuth2AuthorizationRequestRepository;
import com.example.lolserver.member.adapter.in.web.security.JwtAuthenticationFilter;
import com.example.lolserver.member.adapter.in.web.security.LinkOAuth2AuthorizationRequestResolver;
import com.example.lolserver.member.adapter.in.web.security.OAuth2AuthenticationFailureHandler;
import com.example.lolserver.member.adapter.in.web.security.OAuth2AuthenticationSuccessHandler;
import com.example.lolserver.member.adapter.in.web.security.RestAuthenticationEntryPoint;
import com.example.lolserver.member.adapter.in.web.security.SocialAccountLinkTokenStore;
import com.example.lolserver.member.adapter.in.web.security.oauth2.CustomOidcUserService;
import com.nimbusds.jose.JOSEObjectType;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import com.nimbusds.jose.proc.DefaultJOSEObjectTypeVerifier;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.oidc.authentication.OidcIdTokenValidator;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtDecoderFactory;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CorsProperties corsProperties;
    private final OAuth2AuthenticationSuccessHandler oAuth2SuccessHandler;
    private final OAuth2AuthenticationFailureHandler oAuth2FailureHandler;
    private final RedisOAuth2AuthorizationRequestRepository
            redisAuthorizationRequestRepository;
    private final CustomOidcUserService customOidcUserService;
    private final ClientRegistrationRepository clientRegistrationRepository;
    private final SocialAccountLinkTokenStore socialAccountLinkTokenStore;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(
                        corsConfigurationSource()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(this::authorizeRequests)
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(
                                restAuthenticationEntryPoint))
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(authorization -> authorization
                                .baseUri("/oauth2/authorize")
                                .authorizationRequestRepository(
                                        redisAuthorizationRequestRepository)
                                .authorizationRequestResolver(
                                        new LinkOAuth2AuthorizationRequestResolver(
                                                clientRegistrationRepository,
                                                "/oauth2/authorize",
                                                socialAccountLinkTokenStore)))
                        .userInfoEndpoint(userInfo -> userInfo
                                .oidcUserService(customOidcUserService))
                        .successHandler(oAuth2SuccessHandler)
                        .failureHandler(oAuth2FailureHandler)
                )
                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 리소스별 인가 규칙. 먼저 일치한 규칙이 이긴다.
     *
     * <p>{@code /api/v1/**} 를 일괄 {@code permitAll} 하면 회원·커뮤니티·듀오를 {@code /api/v1}
     * 로 옮기는 순간 아래 {@code authenticated()} 규칙이 모두 무력화된다. 그래서 공개 범위를
     * 최상위 리소스 단위로 명시한다. 옛 platformId-prefix 경로({@code /api/v1/*}{@code /rank} 등)는
     * 와일드카드가 다른 리소스를 덮지 않도록 회원·커뮤니티·듀오 규칙 <b>뒤에</b> 둔다.
     */
    private void authorizeRequests(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>
                    .AuthorizationManagerRequestMatcherRegistry auth) {
        auth
                .requestMatchers("/api/auth/logout", "/api/v1/auth/logout")
                        .authenticated()
                .requestMatchers("/api/auth/**", "/api/v1/auth/**").permitAll()
                .requestMatchers("/login/oauth2/**", "/oauth2/**").permitAll()
                .requestMatchers("/docs/**", "/swagger-ui/**",
                        "/v3/api-docs/**").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // Riot 데이터 — 공개 조회
                .requestMatchers(HttpMethod.GET,
                        "/api/v1/summoners/**", "/api/v1/matches/**",
                        "/api/v1/rankings/**", "/api/v1/champion-stats/**",
                        "/api/v1/game-data/**").permitAll()
                // 갱신 요청은 비로그인 사용자도 할 수 있다 (옛 GET .../renewal 과 동일)
                .requestMatchers(HttpMethod.POST,
                        "/api/v1/summoners/*/renewal").permitAll()

                // 커뮤니티
                .requestMatchers(HttpMethod.GET,
                        "/api/community/posts", "/api/community/posts/**",
                        "/api/v1/community/posts", "/api/v1/community/posts/**")
                        .permitAll()
                // 게시판 목록은 비로그인 사용자에게도 사이드바를 그리는 데 필요하다.
                .requestMatchers(HttpMethod.GET,
                        "/api/community/categories",
                        "/api/v1/community/categories").permitAll()
                .requestMatchers("/api/community/**", "/api/v1/community/**")
                        .authenticated()

                // 듀오
                .requestMatchers(HttpMethod.GET,
                        "/api/duo/posts/*/match-result",
                        "/api/duo/posts/*/requests",
                        "/api/v1/duo/posts/*/match-result",
                        "/api/v1/duo/posts/*/requests").authenticated()
                .requestMatchers(HttpMethod.GET,
                        "/api/duo/posts", "/api/duo/posts/**",
                        "/api/v1/duo/posts", "/api/v1/duo/posts/**")
                        .permitAll()
                .requestMatchers("/api/duo/**", "/api/v1/duo/**").authenticated()

                // 회원
                .requestMatchers("/api/members/**", "/api/v1/members/**")
                        .authenticated()
                .requestMatchers("/api/admin/**", "/api/v1/admin/**")
                        .hasRole("ADMIN")

                // 옛 Riot 데이터 경로 — lol-ui 전환 후 MP-156 에서 제거
                .requestMatchers(HttpMethod.GET,
                        "/api/v1/*/rank", "/api/v1/*/tier-cutoffs/**",
                        "/api/v1/*/champion-stats/**", "/api/v1/*/champion/**",
                        "/api/v1/*/spectator/**", "/api/v1/*/summoners/**",
                        "/api/v1/*/matches/**", "/api/v1/match/timeline/**",
                        "/api/v1/leagues/**", "/api/v1/rank/champions",
                        "/api/v1/versions/**", "/api/v1/seasons/**",
                        "/api/v1/patch-notes/**", "/api/v1/queue-tab")
                        .permitAll()
                .anyRequest().authenticated();
    }

    @Bean
    public JwtDecoderFactory<ClientRegistration> idTokenDecoderFactory() {
        return clientRegistration -> {
            String jwkSetUri = clientRegistration.getProviderDetails()
                    .getJwkSetUri();

            NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder
                    .withJwkSetUri(jwkSetUri)
                    .jwtProcessorCustomizer(processor ->
                            processor.setJWSTypeVerifier(
                                    new DefaultJOSEObjectTypeVerifier<>(
                                            JOSEObjectType.JWT,
                                            new JOSEObjectType("id_token+jwt"),
                                            null)))
                    .build();

            jwtDecoder.setJwtValidator(
                    new DelegatingOAuth2TokenValidator<>(
                            new JwtTimestampValidator(),
                            new OidcIdTokenValidator(clientRegistration)));

            return jwtDecoder;
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(
                corsProperties.getAllowedOrigins());
        configuration.setAllowedMethods(
                List.of("GET", "POST", "PUT", "PATCH",
                        "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(
                List.of("Content-Type", "Authorization",
                        "X-Requested-With"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
