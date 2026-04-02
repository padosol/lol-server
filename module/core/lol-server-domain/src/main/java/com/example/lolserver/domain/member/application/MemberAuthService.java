package com.example.lolserver.domain.member.application;

import com.example.lolserver.domain.member.application.dto.OAuthLoginCommand;
import com.example.lolserver.domain.member.application.dto.TokenRefreshCommand;
import com.example.lolserver.domain.member.application.model.AuthTokenReadModel;
import com.example.lolserver.domain.member.application.model.OAuthUserInfo;
import com.example.lolserver.domain.member.application.port.in.MemberAuthUseCase;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberAuthService implements MemberAuthUseCase {

    private final MemberPersistencePort memberPersistencePort;
    private final OAuthClientPort oAuthClientPort;
    private final TokenPort tokenPort;
    private final RefreshTokenPort refreshTokenPort;
    private final OAuthStatePort oAuthStatePort;
    private final OAuthAuthorizationPort oAuthAuthorizationPort;

    @Override
    public String getOAuthAuthorizationUrl(OAuthProvider provider) {
        String state = UUID.randomUUID().toString();
        oAuthStatePort.saveState(state, 300);
        return oAuthAuthorizationPort.buildAuthorizationUrl(
                provider, state);
    }

    @Override
    @Transactional
    public AuthTokenReadModel loginWithOAuth(OAuthLoginCommand command) {
        if (command.getState() != null) {
            if (!oAuthStatePort.validateAndDelete(command.getState())) {
                throw new CoreException(ErrorType.INVALID_OAUTH_STATE);
            }
        }

        String redirectUri = command.getRedirectUri() != null
                ? command.getRedirectUri()
                : oAuthAuthorizationPort.getCallbackUri(
                        command.getProvider());

        OAuthUserInfo userInfo = oAuthClientPort.getUserInfo(
                command.getProvider(), command.getCode(), redirectUri);

        return findOrCreateMemberAndGenerateTokens(userInfo);
    }

    @Override
    @Transactional
    public AuthTokenReadModel loginWithOAuthUserInfo(
            OAuthUserInfo userInfo) {
        return findOrCreateMemberAndGenerateTokens(userInfo);
    }

    @Override
    @Transactional
    public AuthTokenReadModel refreshToken(
            TokenRefreshCommand command) {
        String refreshToken = command.getRefreshToken();

        if (!tokenPort.validateToken(refreshToken)) {
            throw new CoreException(ErrorType.INVALID_TOKEN);
        }

        Long memberId = tokenPort.getMemberIdFromToken(refreshToken);

        String savedToken = refreshTokenPort.find(memberId)
                .orElseThrow(() -> new CoreException(
                        ErrorType.EXPIRED_TOKEN));

        if (!savedToken.equals(refreshToken)) {
            throw new CoreException(ErrorType.INVALID_TOKEN);
        }

        Member member = memberPersistencePort.findById(memberId)
                .orElseThrow(() -> new CoreException(
                        ErrorType.MEMBER_NOT_FOUND));

        return generateTokens(member);
    }

    @Override
    @Transactional
    public void logout(Long memberId) {
        refreshTokenPort.delete(memberId);
    }

    private AuthTokenReadModel findOrCreateMemberAndGenerateTokens(
            OAuthUserInfo userInfo) {
        Member member = memberPersistencePort
                .findByOAuthProviderAndProviderId(
                        userInfo.getProvider(), userInfo.getProviderId())
                .orElse(null);

        if (member == null) {
            member = createMember(userInfo);
        } else {
            member.updateLastLogin();
            memberPersistencePort.save(member);
        }

        return generateTokens(member);
    }

    private Member createMember(OAuthUserInfo userInfo) {
        Member member = Member.createFromOAuth(
                userInfo.getEmail(),
                userInfo.getNickname(),
                userInfo.getProfileImageUrl(),
                userInfo.getProvider(),
                userInfo.getProviderId());
        return memberPersistencePort.save(member);
    }

    private AuthTokenReadModel generateTokens(Member member) {
        String accessToken = tokenPort.generateAccessToken(
                member.getId(), member.getRole());
        String refreshToken = tokenPort.generateRefreshToken(
                member.getId(), member.getRole());

        refreshTokenPort.save(member.getId(), refreshToken,
                tokenPort.getRefreshTokenExpiry());

        return new AuthTokenReadModel(accessToken, refreshToken,
                tokenPort.getAccessTokenExpiry());
    }
}
