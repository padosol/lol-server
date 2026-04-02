package com.example.lolserver.domain.member.application;

import com.example.lolserver.domain.member.application.dto.OAuthLoginCommand;
import com.example.lolserver.domain.member.application.dto.TokenRefreshCommand;
import com.example.lolserver.domain.member.application.model.AuthTokenReadModel;
import com.example.lolserver.domain.member.application.model.OAuthUserInfo;
import com.example.lolserver.domain.member.application.port.out.MemberPersistencePort;
import com.example.lolserver.domain.member.application.port.out.OAuthAuthorizationPort;
import com.example.lolserver.domain.member.application.port.out.OAuthClientPort;
import com.example.lolserver.domain.member.application.port.out.OAuthStatePort;
import com.example.lolserver.domain.member.application.port.out.RefreshTokenPort;
import com.example.lolserver.domain.member.application.port.out.TokenPort;
import com.example.lolserver.domain.member.domain.Member;
import com.example.lolserver.domain.member.domain.vo.OAuthProvider;
import com.example.lolserver.support.error.CoreException;
import com.example.lolserver.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class MemberAuthServiceTest {

    @Mock
    private MemberPersistencePort memberPersistencePort;

    @Mock
    private OAuthClientPort oAuthClientPort;

    @Mock
    private TokenPort tokenPort;

    @Mock
    private RefreshTokenPort refreshTokenPort;

    @Mock
    private OAuthStatePort oAuthStatePort;

    @Mock
    private OAuthAuthorizationPort oAuthAuthorizationPort;

    @InjectMocks
    private MemberAuthService memberAuthService;

    @DisplayName("기존 회원이 Google OAuth 로그인하면 토큰을 반환한다")
    @Test
    void loginWithOAuth_existingMember() {
        // given
        OAuthLoginCommand command = OAuthLoginCommand.builder()
                .provider(OAuthProvider.GOOGLE)
                .code("auth-code")
                .redirectUri("http://localhost:3000/callback")
                .build();

        OAuthUserInfo userInfo = OAuthUserInfo.builder()
                .provider("GOOGLE")
                .providerId("google-123")
                .email("test@gmail.com")
                .nickname("테스터")
                .build();

        Member existingMember = new Member(1L, "test@gmail.com", "테스터", null,
                "GOOGLE", "google-123", "USER", LocalDateTime.now(), null);

        given(oAuthClientPort.getUserInfo(OAuthProvider.GOOGLE, "auth-code", "http://localhost:3000/callback"))
                .willReturn(userInfo);
        given(memberPersistencePort.findByOAuthProviderAndProviderId("GOOGLE", "google-123"))
                .willReturn(Optional.of(existingMember));
        given(memberPersistencePort.save(any(Member.class))).willReturn(existingMember);
        given(tokenPort.generateAccessToken(1L, "USER")).willReturn("access-token");
        given(tokenPort.generateRefreshToken(1L, "USER")).willReturn("refresh-token");
        given(tokenPort.getAccessTokenExpiry()).willReturn(1800L);
        given(tokenPort.getRefreshTokenExpiry()).willReturn(1209600L);

        // when
        AuthTokenReadModel result = memberAuthService.loginWithOAuth(command);

        // then
        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        assertThat(result.expiresIn()).isEqualTo(1800L);
    }

    @DisplayName("신규 회원이 Google OAuth 로그인하면 회원가입 후 토큰을 반환한다")
    @Test
    void loginWithOAuth_newMember() {
        // given
        OAuthLoginCommand command = OAuthLoginCommand.builder()
                .provider(OAuthProvider.GOOGLE)
                .code("auth-code")
                .redirectUri("http://localhost:3000/callback")
                .build();

        OAuthUserInfo userInfo = OAuthUserInfo.builder()
                .provider("GOOGLE")
                .providerId("google-new")
                .email("new@gmail.com")
                .nickname("신규유저")
                .build();

        Member savedMember = new Member(2L, "new@gmail.com", "신규유저", null,
                "GOOGLE", "google-new", "USER", LocalDateTime.now(), null);

        given(oAuthClientPort.getUserInfo(OAuthProvider.GOOGLE, "auth-code", "http://localhost:3000/callback"))
                .willReturn(userInfo);
        given(memberPersistencePort.findByOAuthProviderAndProviderId("GOOGLE", "google-new"))
                .willReturn(Optional.empty());
        given(memberPersistencePort.save(any(Member.class))).willReturn(savedMember);
        given(tokenPort.generateAccessToken(2L, "USER")).willReturn("access-token");
        given(tokenPort.generateRefreshToken(2L, "USER")).willReturn("refresh-token");
        given(tokenPort.getAccessTokenExpiry()).willReturn(1800L);
        given(tokenPort.getRefreshTokenExpiry()).willReturn(1209600L);

        // when
        AuthTokenReadModel result = memberAuthService.loginWithOAuth(command);

        // then
        assertThat(result.accessToken()).isEqualTo("access-token");
        then(memberPersistencePort).should(times(1)).save(any(Member.class));
    }

    @DisplayName("Spring Security OAuth2로 기존 회원 로그인 시 토큰을 반환한다")
    @Test
    void loginWithOAuthUserInfo_existingMember() {
        // given
        OAuthUserInfo userInfo = OAuthUserInfo.builder()
                .provider("GOOGLE")
                .providerId("google-123")
                .email("test@gmail.com")
                .nickname("테스터")
                .profileImageUrl("https://example.com/photo.jpg")
                .build();

        Member existingMember = new Member(1L, "test@gmail.com", "테스터",
                null, "GOOGLE", "google-123", "USER",
                LocalDateTime.now(), null);

        given(memberPersistencePort.findByOAuthProviderAndProviderId(
                "GOOGLE", "google-123"))
                .willReturn(Optional.of(existingMember));
        given(memberPersistencePort.save(any(Member.class)))
                .willReturn(existingMember);
        given(tokenPort.generateAccessToken(1L, "USER"))
                .willReturn("access-token");
        given(tokenPort.generateRefreshToken(1L, "USER"))
                .willReturn("refresh-token");
        given(tokenPort.getAccessTokenExpiry()).willReturn(1800L);
        given(tokenPort.getRefreshTokenExpiry()).willReturn(1209600L);

        // when
        AuthTokenReadModel result =
                memberAuthService.loginWithOAuthUserInfo(userInfo);

        // then
        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        then(memberPersistencePort).should().save(any(Member.class));
    }

    @DisplayName("Spring Security OAuth2로 신규 회원 로그인 시 회원가입 후 토큰을 반환한다")
    @Test
    void loginWithOAuthUserInfo_newMember() {
        // given
        OAuthUserInfo userInfo = OAuthUserInfo.builder()
                .provider("GOOGLE")
                .providerId("google-new")
                .email("new@gmail.com")
                .nickname("신규유저")
                .build();

        Member savedMember = new Member(2L, "new@gmail.com", "신규유저",
                null, "GOOGLE", "google-new", "USER",
                LocalDateTime.now(), null);

        given(memberPersistencePort.findByOAuthProviderAndProviderId(
                "GOOGLE", "google-new"))
                .willReturn(Optional.empty());
        given(memberPersistencePort.save(any(Member.class)))
                .willReturn(savedMember);
        given(tokenPort.generateAccessToken(2L, "USER"))
                .willReturn("access-token");
        given(tokenPort.generateRefreshToken(2L, "USER"))
                .willReturn("refresh-token");
        given(tokenPort.getAccessTokenExpiry()).willReturn(1800L);
        given(tokenPort.getRefreshTokenExpiry()).willReturn(1209600L);

        // when
        AuthTokenReadModel result =
                memberAuthService.loginWithOAuthUserInfo(userInfo);

        // then
        assertThat(result.accessToken()).isEqualTo("access-token");
        then(memberPersistencePort).should().save(any(Member.class));
    }

    @DisplayName("유효한 리프레시 토큰으로 갱신하면 새 토큰을 반환한다")
    @Test
    void refreshToken_validToken() {
        // given
        TokenRefreshCommand command = TokenRefreshCommand.builder()
                .refreshToken("valid-refresh-token")
                .build();

        Member member = new Member(1L, "test@gmail.com", "테스터", null,
                "GOOGLE", "google-123", "USER", LocalDateTime.now(), null);

        given(tokenPort.validateToken("valid-refresh-token")).willReturn(true);
        given(tokenPort.getMemberIdFromToken("valid-refresh-token")).willReturn(1L);
        given(refreshTokenPort.find(1L)).willReturn(Optional.of("valid-refresh-token"));
        given(memberPersistencePort.findById(1L)).willReturn(Optional.of(member));
        given(tokenPort.generateAccessToken(1L, "USER")).willReturn("new-access-token");
        given(tokenPort.generateRefreshToken(1L, "USER")).willReturn("new-refresh-token");
        given(tokenPort.getAccessTokenExpiry()).willReturn(1800L);
        given(tokenPort.getRefreshTokenExpiry()).willReturn(1209600L);

        // when
        AuthTokenReadModel result = memberAuthService.refreshToken(command);

        // then
        assertThat(result.accessToken()).isEqualTo("new-access-token");
        assertThat(result.refreshToken()).isEqualTo("new-refresh-token");
    }

    @DisplayName("유효하지 않은 리프레시 토큰으로 갱신하면 예외가 발생한다")
    @Test
    void refreshToken_invalidToken() {
        // given
        TokenRefreshCommand command = TokenRefreshCommand.builder()
                .refreshToken("invalid-token")
                .build();

        given(tokenPort.validateToken("invalid-token")).willReturn(false);

        // when & then
        assertThatThrownBy(() -> memberAuthService.refreshToken(command))
                .isInstanceOf(CoreException.class)
                .extracting(e -> ((CoreException) e).getErrorType())
                .isEqualTo(ErrorType.INVALID_TOKEN);
    }

    @DisplayName("저장된 리프레시 토큰과 불일치하면 예외가 발생한다")
    @Test
    void refreshToken_mismatchToken() {
        // given
        TokenRefreshCommand command = TokenRefreshCommand.builder()
                .refreshToken("mismatched-token")
                .build();

        given(tokenPort.validateToken("mismatched-token")).willReturn(true);
        given(tokenPort.getMemberIdFromToken("mismatched-token")).willReturn(1L);
        given(refreshTokenPort.find(1L)).willReturn(Optional.of("different-token"));

        // when & then
        assertThatThrownBy(() -> memberAuthService.refreshToken(command))
                .isInstanceOf(CoreException.class)
                .extracting(e -> ((CoreException) e).getErrorType())
                .isEqualTo(ErrorType.INVALID_TOKEN);
    }

    @DisplayName("로그아웃하면 리프레시 토큰이 삭제된다")
    @Test
    void logout() {
        // when
        memberAuthService.logout(1L);

        // then
        then(refreshTokenPort).should().delete(1L);
    }

    @DisplayName("OAuth 인가 URL을 생성하면 state를 저장하고 URL을 반환한다")
    @Test
    void getOAuthAuthorizationUrl_success() {
        // given
        given(oAuthAuthorizationPort.buildAuthorizationUrl(eq(OAuthProvider.GOOGLE), anyString()))
                .willReturn("https://accounts.google.com/o/oauth2/v2/auth?state=test");

        // when
        String url = memberAuthService.getOAuthAuthorizationUrl(OAuthProvider.GOOGLE);

        // then
        assertThat(url).contains("accounts.google.com");
        then(oAuthStatePort).should().saveState(anyString(), eq(300L));
    }

    @DisplayName("유효한 state로 OAuth 로그인하면 토큰을 반환한다")
    @Test
    void loginWithOAuth_validState_success() {
        // given
        OAuthLoginCommand command = OAuthLoginCommand.builder()
                .provider(OAuthProvider.GOOGLE)
                .code("auth-code")
                .state("valid-state")
                .redirectUri("http://localhost:3000/callback")
                .build();

        OAuthUserInfo userInfo = OAuthUserInfo.builder()
                .provider("GOOGLE")
                .providerId("google-123")
                .email("test@gmail.com")
                .nickname("테스터")
                .build();

        Member existingMember = new Member(1L, "test@gmail.com", "테스터", null,
                "GOOGLE", "google-123", "USER", LocalDateTime.now(), null);

        given(oAuthStatePort.validateAndDelete("valid-state")).willReturn(true);
        given(oAuthClientPort.getUserInfo(OAuthProvider.GOOGLE, "auth-code",
                "http://localhost:3000/callback"))
                .willReturn(userInfo);
        given(memberPersistencePort.findByOAuthProviderAndProviderId("GOOGLE", "google-123"))
                .willReturn(Optional.of(existingMember));
        given(memberPersistencePort.save(any(Member.class))).willReturn(existingMember);
        given(tokenPort.generateAccessToken(1L, "USER")).willReturn("access-token");
        given(tokenPort.generateRefreshToken(1L, "USER")).willReturn("refresh-token");
        given(tokenPort.getAccessTokenExpiry()).willReturn(1800L);
        given(tokenPort.getRefreshTokenExpiry()).willReturn(1209600L);

        // when
        AuthTokenReadModel result = memberAuthService.loginWithOAuth(command);

        // then
        assertThat(result.accessToken()).isEqualTo("access-token");
        then(oAuthStatePort).should().validateAndDelete("valid-state");
    }

    @DisplayName("유효하지 않은 state로 OAuth 로그인하면 예외가 발생한다")
    @Test
    void loginWithOAuth_invalidState_throwsException() {
        // given
        OAuthLoginCommand command = OAuthLoginCommand.builder()
                .provider(OAuthProvider.GOOGLE)
                .code("auth-code")
                .state("invalid-state")
                .build();

        given(oAuthStatePort.validateAndDelete("invalid-state")).willReturn(false);

        // when & then
        assertThatThrownBy(() -> memberAuthService.loginWithOAuth(command))
                .isInstanceOf(CoreException.class)
                .extracting(e -> ((CoreException) e).getErrorType())
                .isEqualTo(ErrorType.INVALID_OAUTH_STATE);
    }
}
