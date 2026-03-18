package com.example.lolserver.domain.member.application;

import com.example.lolserver.domain.member.application.dto.OAuthLoginCommand;
import com.example.lolserver.domain.member.application.dto.RiotLinkCommand;
import com.example.lolserver.domain.member.application.dto.TokenRefreshCommand;
import com.example.lolserver.domain.member.application.model.AuthTokenReadModel;
import com.example.lolserver.domain.member.application.model.MemberReadModel;
import com.example.lolserver.domain.member.application.model.OAuthUserInfo;
import com.example.lolserver.domain.member.application.model.RiotAccountLinkReadModel;
import com.example.lolserver.domain.member.application.port.out.MemberPersistencePort;
import com.example.lolserver.domain.member.application.port.out.OAuthAuthorizationPort;
import com.example.lolserver.domain.member.application.port.out.OAuthClientPort;
import com.example.lolserver.domain.member.application.port.out.OAuthStatePort;
import com.example.lolserver.domain.member.application.port.out.RefreshTokenPort;
import com.example.lolserver.domain.member.application.port.out.RiotAccountLinkPersistencePort;
import com.example.lolserver.domain.member.application.port.out.TokenPort;
import com.example.lolserver.domain.member.domain.Member;
import com.example.lolserver.domain.member.domain.RiotAccountLink;
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
import java.util.List;
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
class MemberServiceTest {

    @Mock
    private MemberPersistencePort memberPersistencePort;

    @Mock
    private RiotAccountLinkPersistencePort riotAccountLinkPersistencePort;

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
    private MemberService memberService;

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
        AuthTokenReadModel result = memberService.loginWithOAuth(command);

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
        AuthTokenReadModel result = memberService.loginWithOAuth(command);

        // then
        assertThat(result.accessToken()).isEqualTo("access-token");
        then(memberPersistencePort).should(times(1)).save(any(Member.class));
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
        AuthTokenReadModel result = memberService.refreshToken(command);

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
        assertThatThrownBy(() -> memberService.refreshToken(command))
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
        assertThatThrownBy(() -> memberService.refreshToken(command))
                .isInstanceOf(CoreException.class)
                .extracting(e -> ((CoreException) e).getErrorType())
                .isEqualTo(ErrorType.INVALID_TOKEN);
    }

    @DisplayName("로그아웃하면 리프레시 토큰이 삭제된다")
    @Test
    void logout() {
        // when
        memberService.logout(1L);

        // then
        then(refreshTokenPort).should().delete(1L);
    }

    @DisplayName("Riot 계정을 연동하면 연동 정보를 반환한다")
    @Test
    void linkRiotAccount() {
        // given
        Long memberId = 1L;
        RiotLinkCommand command = RiotLinkCommand.builder()
                .code("riot-code")
                .redirectUri("http://localhost:3000/riot-callback")
                .platformId("KR")
                .build();

        OAuthUserInfo riotInfo = OAuthUserInfo.builder()
                .provider("RIOT")
                .puuid("test-puuid")
                .gameName("Player")
                .tagLine("KR1")
                .build();

        RiotAccountLink savedLink = new RiotAccountLink(
                1L, memberId, "test-puuid", "Player", "KR1", "KR", LocalDateTime.now());

        given(oAuthClientPort.getUserInfo(eq(OAuthProvider.RIOT), eq("riot-code"),
                eq("http://localhost:3000/riot-callback")))
                .willReturn(riotInfo);
        given(riotAccountLinkPersistencePort.findByMemberIdAndPuuid(memberId, "test-puuid"))
                .willReturn(Optional.empty());
        given(riotAccountLinkPersistencePort.save(any(RiotAccountLink.class)))
                .willReturn(savedLink);

        // when
        RiotAccountLinkReadModel result = memberService.linkRiotAccount(memberId, command);

        // then
        assertThat(result.getPuuid()).isEqualTo("test-puuid");
        assertThat(result.getGameName()).isEqualTo("Player");
    }

    @DisplayName("이미 연동된 Riot 계정을 다시 연동하면 예외가 발생한다")
    @Test
    void linkRiotAccount_alreadyLinked() {
        // given
        Long memberId = 1L;
        RiotLinkCommand command = RiotLinkCommand.builder()
                .code("riot-code")
                .redirectUri("http://localhost:3000/riot-callback")
                .platformId("KR")
                .build();

        OAuthUserInfo riotInfo = OAuthUserInfo.builder()
                .provider("RIOT")
                .puuid("test-puuid")
                .gameName("Player")
                .tagLine("KR1")
                .build();

        RiotAccountLink existingLink = new RiotAccountLink(
                1L, memberId, "test-puuid", "Player", "KR1", "KR", LocalDateTime.now());

        given(oAuthClientPort.getUserInfo(eq(OAuthProvider.RIOT), anyString(), anyString()))
                .willReturn(riotInfo);
        given(riotAccountLinkPersistencePort.findByMemberIdAndPuuid(memberId, "test-puuid"))
                .willReturn(Optional.of(existingLink));

        // when & then
        assertThatThrownBy(() -> memberService.linkRiotAccount(memberId, command))
                .isInstanceOf(CoreException.class)
                .extracting(e -> ((CoreException) e).getErrorType())
                .isEqualTo(ErrorType.RIOT_ACCOUNT_ALREADY_LINKED);
    }

    @DisplayName("내 프로필을 조회하면 회원 정보를 반환한다")
    @Test
    void getMyProfile() {
        // given
        Long memberId = 1L;
        Member member = new Member(1L, "test@gmail.com", "테스터", null,
                "GOOGLE", "google-123", "USER", LocalDateTime.now(), null);

        given(memberPersistencePort.findById(memberId)).willReturn(Optional.of(member));

        // when
        MemberReadModel result = memberService.getMyProfile(memberId);

        // then
        assertThat(result.getEmail()).isEqualTo("test@gmail.com");
        assertThat(result.getNickname()).isEqualTo("테스터");
    }

    @DisplayName("존재하지 않는 회원의 프로필을 조회하면 예외가 발생한다")
    @Test
    void getMyProfile_notFound() {
        // given
        given(memberPersistencePort.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> memberService.getMyProfile(999L))
                .isInstanceOf(CoreException.class)
                .extracting(e -> ((CoreException) e).getErrorType())
                .isEqualTo(ErrorType.MEMBER_NOT_FOUND);
    }

    @DisplayName("연동된 Riot 계정 목록을 조회하면 목록을 반환한다")
    @Test
    void getLinkedAccounts() {
        // given
        Long memberId = 1L;
        List<RiotAccountLink> links = List.of(
                new RiotAccountLink(1L, memberId, "puuid-1", "Player1", "KR1", "KR",
                        LocalDateTime.now()),
                new RiotAccountLink(2L, memberId, "puuid-2", "Player2", "NA1", "NA1",
                        LocalDateTime.now())
        );

        given(riotAccountLinkPersistencePort.findByMemberId(memberId)).willReturn(links);

        // when
        List<RiotAccountLinkReadModel> result = memberService.getLinkedAccounts(memberId);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getPuuid()).isEqualTo("puuid-1");
    }

    @DisplayName("Riot 계정 연동을 해제한다")
    @Test
    void unlinkRiotAccount() {
        // given
        Long memberId = 1L;
        Long linkId = 1L;
        RiotAccountLink link = new RiotAccountLink(
                linkId, memberId, "puuid-1", "Player1", "KR1", "KR", LocalDateTime.now());

        given(riotAccountLinkPersistencePort.findByIdAndMemberId(linkId, memberId))
                .willReturn(Optional.of(link));

        // when
        memberService.unlinkRiotAccount(memberId, linkId);

        // then
        then(riotAccountLinkPersistencePort).should().delete(link);
    }

    @DisplayName("존재하지 않는 Riot 연동을 해제하면 예외가 발생한다")
    @Test
    void unlinkRiotAccount_notFound() {
        // given
        Long memberId = 1L;
        Long linkId = 999L;

        given(riotAccountLinkPersistencePort.findByIdAndMemberId(linkId, memberId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> memberService.unlinkRiotAccount(memberId, linkId))
                .isInstanceOf(CoreException.class)
                .extracting(e -> ((CoreException) e).getErrorType())
                .isEqualTo(ErrorType.RIOT_LINK_NOT_FOUND);
    }

    @DisplayName("OAuth 인가 URL을 생성하면 state를 저장하고 URL을 반환한다")
    @Test
    void getOAuthAuthorizationUrl_success() {
        // given
        given(oAuthAuthorizationPort.buildAuthorizationUrl(eq(OAuthProvider.GOOGLE), anyString()))
                .willReturn("https://accounts.google.com/o/oauth2/v2/auth?state=test");

        // when
        String url = memberService.getOAuthAuthorizationUrl(OAuthProvider.GOOGLE);

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
        given(oAuthAuthorizationPort.getCallbackUri(OAuthProvider.GOOGLE))
                .willReturn("http://localhost:8100/api/auth/google/callback");
        given(oAuthClientPort.getUserInfo(OAuthProvider.GOOGLE, "auth-code",
                "http://localhost:8100/api/auth/google/callback"))
                .willReturn(userInfo);
        given(memberPersistencePort.findByOAuthProviderAndProviderId("GOOGLE", "google-123"))
                .willReturn(Optional.of(existingMember));
        given(memberPersistencePort.save(any(Member.class))).willReturn(existingMember);
        given(tokenPort.generateAccessToken(1L, "USER")).willReturn("access-token");
        given(tokenPort.generateRefreshToken(1L, "USER")).willReturn("refresh-token");
        given(tokenPort.getAccessTokenExpiry()).willReturn(1800L);
        given(tokenPort.getRefreshTokenExpiry()).willReturn(1209600L);

        // when
        AuthTokenReadModel result = memberService.loginWithOAuth(command);

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
        assertThatThrownBy(() -> memberService.loginWithOAuth(command))
                .isInstanceOf(CoreException.class)
                .extracting(e -> ((CoreException) e).getErrorType())
                .isEqualTo(ErrorType.INVALID_OAUTH_STATE);
    }
}
