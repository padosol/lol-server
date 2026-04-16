package com.example.lolserver.domain.member.application;

import com.example.lolserver.domain.member.application.dto.OAuthLoginCommand;
import com.example.lolserver.domain.member.application.dto.TokenRefreshCommand;
import com.example.lolserver.domain.member.application.model.AuthTokenReadModel;
import com.example.lolserver.domain.member.application.model.OAuthUserInfo;
import com.example.lolserver.domain.member.application.port.out.MemberPersistencePort;
import com.example.lolserver.domain.member.application.port.out.MemberWithdrawalPersistencePort;
import com.example.lolserver.domain.member.application.port.out.OAuthAuthorizationPort;
import com.example.lolserver.domain.member.application.port.out.OAuthClientPort;
import com.example.lolserver.domain.member.application.port.out.OAuthStatePort;
import com.example.lolserver.domain.member.application.port.out.RefreshTokenPort;
import com.example.lolserver.domain.member.application.port.out.SocialAccountPersistencePort;
import com.example.lolserver.domain.member.application.port.out.TokenPort;
import com.example.lolserver.domain.member.domain.Member;
import com.example.lolserver.domain.member.domain.MemberWithdrawal;
import com.example.lolserver.domain.member.domain.SocialAccount;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class MemberAuthServiceTest {

    @Mock
    private MemberPersistencePort memberPersistencePort;

    @Mock
    private SocialAccountPersistencePort socialAccountPersistencePort;

    @Mock
    private MemberWithdrawalPersistencePort memberWithdrawalPersistencePort;

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

        SocialAccount existingSocialAccount = SocialAccount.builder()
                .id(1L).memberId(1L)
                .provider("GOOGLE").providerId("google-123")
                .build();

        Member existingMember = Member.builder()
                .id(1L).uuid("test-uuid").email("test@gmail.com")
                .nickname("테스터").role("USER")
                .createdAt(LocalDateTime.now()).build();

        given(oAuthClientPort.getUserInfo(OAuthProvider.GOOGLE,
                "auth-code", "http://localhost:3000/callback"))
                .willReturn(userInfo);
        given(socialAccountPersistencePort.findByProviderAndProviderId(
                "GOOGLE", "google-123"))
                .willReturn(Optional.of(existingSocialAccount));
        given(memberPersistencePort.findById(1L))
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
                memberAuthService.loginWithOAuth(command);

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

        Member savedMember = Member.builder()
                .id(2L).uuid("new-uuid")
                .nickname("용감한소환사1234").role("USER")
                .createdAt(LocalDateTime.now()).build();

        given(oAuthClientPort.getUserInfo(OAuthProvider.GOOGLE,
                "auth-code", "http://localhost:3000/callback"))
                .willReturn(userInfo);
        given(socialAccountPersistencePort.findByProviderAndProviderId(
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
                memberAuthService.loginWithOAuth(command);

        // then
        assertThat(result.accessToken()).isEqualTo("access-token");
        then(memberPersistencePort).should().save(any(Member.class));
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
                .build();

        SocialAccount existingSocialAccount = SocialAccount.builder()
                .id(1L).memberId(1L)
                .provider("GOOGLE").providerId("google-123").build();

        Member existingMember = Member.builder()
                .id(1L).uuid("test-uuid").email("test@gmail.com")
                .nickname("테스터").role("USER")
                .createdAt(LocalDateTime.now()).build();

        given(socialAccountPersistencePort.findByProviderAndProviderId(
                "GOOGLE", "google-123"))
                .willReturn(Optional.of(existingSocialAccount));
        given(memberPersistencePort.findById(1L))
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

        Member savedMember = Member.builder()
                .id(2L).uuid("new-uuid")
                .nickname("빛나는전사5678").role("USER")
                .createdAt(LocalDateTime.now()).build();

        given(socialAccountPersistencePort.findByProviderAndProviderId(
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

        Member member = Member.builder()
                .id(1L).uuid("test-uuid").email("test@gmail.com")
                .nickname("테스터").role("USER")
                .createdAt(LocalDateTime.now()).build();

        given(tokenPort.validateToken("valid-refresh-token"))
                .willReturn(true);
        given(tokenPort.getMemberIdFromToken("valid-refresh-token"))
                .willReturn(1L);
        given(refreshTokenPort.find(1L))
                .willReturn(Optional.of("valid-refresh-token"));
        given(memberPersistencePort.findById(1L))
                .willReturn(Optional.of(member));
        given(tokenPort.generateAccessToken(1L, "USER"))
                .willReturn("new-access-token");
        given(tokenPort.generateRefreshToken(1L, "USER"))
                .willReturn("new-refresh-token");
        given(tokenPort.getAccessTokenExpiry()).willReturn(1800L);
        given(tokenPort.getRefreshTokenExpiry()).willReturn(1209600L);

        // when
        AuthTokenReadModel result =
                memberAuthService.refreshToken(command);

        // then
        assertThat(result.accessToken()).isEqualTo("new-access-token");
        assertThat(result.refreshToken())
                .isEqualTo("new-refresh-token");
    }

    @DisplayName("유효하지 않은 리프레시 토큰으로 갱신하면 예외가 발생한다")
    @Test
    void refreshToken_invalidToken() {
        // given
        TokenRefreshCommand command = TokenRefreshCommand.builder()
                .refreshToken("invalid-token")
                .build();

        given(tokenPort.validateToken("invalid-token"))
                .willReturn(false);

        // when & then
        assertThatThrownBy(() ->
                memberAuthService.refreshToken(command))
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

        given(tokenPort.validateToken("mismatched-token"))
                .willReturn(true);
        given(tokenPort.getMemberIdFromToken("mismatched-token"))
                .willReturn(1L);
        given(refreshTokenPort.find(1L))
                .willReturn(Optional.of("different-token"));

        // when & then
        assertThatThrownBy(() ->
                memberAuthService.refreshToken(command))
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
        given(oAuthAuthorizationPort.buildAuthorizationUrl(
                eq(OAuthProvider.GOOGLE), anyString()))
                .willReturn("https://accounts.google.com/o/oauth2/v2/auth?state=test");

        // when
        String url = memberAuthService.getOAuthAuthorizationUrl(
                OAuthProvider.GOOGLE);

        // then
        assertThat(url).contains("accounts.google.com");
        then(oAuthStatePort).should()
                .saveState(anyString(), eq(300L));
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

        SocialAccount existingSocialAccount = SocialAccount.builder()
                .id(1L).memberId(1L)
                .provider("GOOGLE").providerId("google-123").build();

        Member existingMember = Member.builder()
                .id(1L).uuid("test-uuid").email("test@gmail.com")
                .nickname("테스터").role("USER")
                .createdAt(LocalDateTime.now()).build();

        given(oAuthStatePort.validateAndDelete("valid-state"))
                .willReturn(true);
        given(oAuthClientPort.getUserInfo(OAuthProvider.GOOGLE,
                "auth-code", "http://localhost:3000/callback"))
                .willReturn(userInfo);
        given(socialAccountPersistencePort.findByProviderAndProviderId(
                "GOOGLE", "google-123"))
                .willReturn(Optional.of(existingSocialAccount));
        given(memberPersistencePort.findById(1L))
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
                memberAuthService.loginWithOAuth(command);

        // then
        assertThat(result.accessToken()).isEqualTo("access-token");
        then(oAuthStatePort).should()
                .validateAndDelete("valid-state");
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

        given(oAuthStatePort.validateAndDelete("invalid-state"))
                .willReturn(false);

        // when & then
        assertThatThrownBy(() ->
                memberAuthService.loginWithOAuth(command))
                .isInstanceOf(CoreException.class)
                .extracting(e -> ((CoreException) e).getErrorType())
                .isEqualTo(ErrorType.INVALID_OAUTH_STATE);
    }

    @DisplayName("소셜 계정을 연동하면 aggregate root를 통해 저장된다")
    @Test
    void linkSocialAccount_success() {
        // given
        Long memberId = 1L;
        OAuthUserInfo userInfo = OAuthUserInfo.builder()
                .provider("RIOT")
                .providerId("riot-puuid")
                .build();

        Member member = Member.builder()
                .id(memberId).uuid("test-uuid")
                .nickname("테스터").role("USER")
                .socialAccounts(new ArrayList<>())
                .createdAt(LocalDateTime.now()).build();

        given(socialAccountPersistencePort
                .findByProviderAndProviderId("RIOT", "riot-puuid"))
                .willReturn(Optional.empty());
        given(memberPersistencePort
                .findByIdWithSocialAccounts(memberId))
                .willReturn(Optional.of(member));
        given(memberPersistencePort.save(any(Member.class)))
                .willReturn(member);

        // when
        memberAuthService.linkSocialAccount(memberId, userInfo);

        // then
        then(memberPersistencePort).should()
                .save(any(Member.class));
    }

    @DisplayName("이미 연동된 소셜 계정을 연동하면 예외가 발생한다")
    @Test
    void linkSocialAccount_alreadyLinked() {
        // given
        Long memberId = 1L;
        OAuthUserInfo userInfo = OAuthUserInfo.builder()
                .provider("RIOT")
                .providerId("riot-puuid")
                .build();

        SocialAccount existing = SocialAccount.builder()
                .id(1L).memberId(2L)
                .provider("RIOT").providerId("riot-puuid")
                .build();

        given(socialAccountPersistencePort
                .findByProviderAndProviderId("RIOT", "riot-puuid"))
                .willReturn(Optional.of(existing));

        // when & then
        assertThatThrownBy(() ->
                memberAuthService.linkSocialAccount(
                        memberId, userInfo))
                .isInstanceOf(CoreException.class)
                .extracting(e -> ((CoreException) e).getErrorType())
                .isEqualTo(ErrorType.SOCIAL_ACCOUNT_ALREADY_LINKED);

        then(memberPersistencePort).should(never())
                .save(any(Member.class));
    }

    @DisplayName("본인의 소셜 계정을 연동 해제하면 aggregate root를 통해 삭제된다")
    @Test
    void unlinkSocialAccount_success() {
        // given
        Long memberId = 1L;
        Long socialAccountId = 1L;
        SocialAccount account = SocialAccount.builder()
                .id(socialAccountId).memberId(memberId)
                .provider("GOOGLE").providerId("google-123")
                .linkedAt(LocalDateTime.now())
                .build();

        Member member = Member.builder()
                .id(memberId).uuid("test-uuid")
                .nickname("테스터").role("USER")
                .socialAccounts(new ArrayList<>(List.of(account)))
                .createdAt(LocalDateTime.now()).build();

        given(memberPersistencePort
                .findByIdWithSocialAccounts(memberId))
                .willReturn(Optional.of(member));
        given(memberPersistencePort.save(any(Member.class)))
                .willReturn(member);

        // when
        memberAuthService.unlinkSocialAccount(
                memberId, socialAccountId);

        // then
        then(memberPersistencePort).should()
                .save(any(Member.class));
    }

    @DisplayName("소유하지 않은 소셜 계정을 연동 해제하면 예외가 발생한다")
    @Test
    void unlinkSocialAccount_notFound() {
        // given
        Long memberId = 1L;
        Long socialAccountId = 999L;

        Member member = Member.builder()
                .id(memberId).uuid("test-uuid")
                .nickname("테스터").role("USER")
                .socialAccounts(new ArrayList<>())
                .createdAt(LocalDateTime.now()).build();

        given(memberPersistencePort
                .findByIdWithSocialAccounts(memberId))
                .willReturn(Optional.of(member));

        // when & then
        assertThatThrownBy(() ->
                memberAuthService.unlinkSocialAccount(
                        memberId, socialAccountId))
                .isInstanceOf(CoreException.class)
                .extracting(e -> ((CoreException) e).getErrorType())
                .isEqualTo(ErrorType.SOCIAL_ACCOUNT_NOT_FOUND);

        then(memberPersistencePort).should(never())
                .save(any(Member.class));
    }

    @DisplayName("회원 탈퇴 시 개인정보 익명화 후 탈퇴 기록이 저장된다")
    @Test
    void withdraw_success() {
        // given
        Long memberId = 1L;
        SocialAccount account = SocialAccount.builder()
                .id(1L).memberId(memberId)
                .provider("GOOGLE").providerId("google-123")
                .email("test@gmail.com")
                .linkedAt(LocalDateTime.now()).build();

        Member member = Member.builder()
                .id(memberId).uuid("test-uuid")
                .email("test@gmail.com")
                .nickname("테스터").role("USER")
                .socialAccounts(new ArrayList<>(List.of(account)))
                .createdAt(LocalDateTime.now()).build();

        given(memberPersistencePort
                .findByIdWithSocialAccounts(memberId))
                .willReturn(Optional.of(member));
        given(memberPersistencePort.save(any(Member.class)))
                .willReturn(member);
        given(memberWithdrawalPersistencePort
                .save(any(MemberWithdrawal.class)))
                .willAnswer(inv -> inv.getArgument(0));

        // when
        memberAuthService.withdraw(memberId);

        // then
        then(memberPersistencePort).should()
                .save(any(Member.class));
        then(memberWithdrawalPersistencePort).should()
                .save(any(MemberWithdrawal.class));
        then(refreshTokenPort).should().delete(memberId);
    }

    @DisplayName("탈퇴한 회원의 소셜 계정으로 로그인하면 예외가 발생한다")
    @Test
    void loginWithOAuthUserInfo_withdrawnMember() {
        // given
        OAuthUserInfo userInfo = OAuthUserInfo.builder()
                .provider("GOOGLE")
                .providerId("google-123")
                .build();

        SocialAccount existingAccount = SocialAccount.builder()
                .id(1L).memberId(1L)
                .provider("GOOGLE").providerId("google-123")
                .build();

        Member withdrawnMember = Member.builder()
                .id(1L).uuid("test-uuid")
                .nickname("탈퇴한회원_abc12345").role("USER")
                .withdrawnAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now()).build();

        given(socialAccountPersistencePort
                .findByProviderAndProviderId("GOOGLE", "google-123"))
                .willReturn(Optional.of(existingAccount));
        given(memberPersistencePort.findById(1L))
                .willReturn(Optional.of(withdrawnMember));

        // when & then
        assertThatThrownBy(() ->
                memberAuthService.loginWithOAuthUserInfo(userInfo))
                .isInstanceOf(CoreException.class)
                .extracting(e -> ((CoreException) e).getErrorType())
                .isEqualTo(ErrorType.MEMBER_WITHDRAWN);
    }

    @DisplayName("탈퇴 30일 이내 동일 소셜 계정으로 가입하면 예외가 발생한다")
    @Test
    void loginWithOAuthUserInfo_reregistrationRestricted() {
        // given
        OAuthUserInfo userInfo = OAuthUserInfo.builder()
                .provider("GOOGLE")
                .providerId("google-123")
                .build();

        MemberWithdrawal recentWithdrawal = MemberWithdrawal.builder()
                .id(1L).provider("GOOGLE").providerId("google-123")
                .withdrawnAt(LocalDateTime.now().minusDays(10))
                .build();

        given(socialAccountPersistencePort
                .findByProviderAndProviderId("GOOGLE", "google-123"))
                .willReturn(Optional.empty());
        given(memberWithdrawalPersistencePort
                .findByProviderAndProviderId("GOOGLE", "google-123"))
                .willReturn(Optional.of(recentWithdrawal));

        // when & then
        assertThatThrownBy(() ->
                memberAuthService.loginWithOAuthUserInfo(userInfo))
                .isInstanceOf(CoreException.class)
                .extracting(e -> ((CoreException) e).getErrorType())
                .isEqualTo(
                        ErrorType.WITHDRAWAL_REREGISTRATION_RESTRICTED);
    }

    @DisplayName("탈퇴 30일 이후 동일 소셜 계정으로 가입하면 정상 처리된다")
    @Test
    void loginWithOAuthUserInfo_reregistrationAfter30Days() {
        // given
        OAuthUserInfo userInfo = OAuthUserInfo.builder()
                .provider("GOOGLE")
                .providerId("google-123")
                .email("test@gmail.com")
                .nickname("테스터")
                .build();

        MemberWithdrawal oldWithdrawal = MemberWithdrawal.builder()
                .id(1L).provider("GOOGLE").providerId("google-123")
                .withdrawnAt(LocalDateTime.now().minusDays(31))
                .build();

        Member savedMember = Member.builder()
                .id(2L).uuid("new-uuid")
                .nickname("새회원").role("USER")
                .createdAt(LocalDateTime.now()).build();

        given(socialAccountPersistencePort
                .findByProviderAndProviderId("GOOGLE", "google-123"))
                .willReturn(Optional.empty());
        given(memberWithdrawalPersistencePort
                .findByProviderAndProviderId("GOOGLE", "google-123"))
                .willReturn(Optional.of(oldWithdrawal));
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
        then(memberPersistencePort).should()
                .save(any(Member.class));
    }
}
