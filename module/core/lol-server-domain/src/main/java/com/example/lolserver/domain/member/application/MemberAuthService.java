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
import com.example.lolserver.domain.member.application.port.out.SocialAccountPersistencePort;
import com.example.lolserver.domain.member.application.port.out.TokenPort;
import com.example.lolserver.domain.member.domain.Member;
import com.example.lolserver.domain.member.domain.SocialAccount;
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
    private final SocialAccountPersistencePort socialAccountPersistencePort;
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

        OAuthUserInfo userInfo = oAuthClientPort.getUserInfo(
                command.getProvider(), command.getCode(),
                command.getRedirectUri());

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
    public void linkSocialAccount(Long memberId, OAuthUserInfo userInfo) {
        memberPersistencePort.findById(memberId)
                .orElseThrow(() -> new CoreException(
                        ErrorType.MEMBER_NOT_FOUND));

        socialAccountPersistencePort
                .findByProviderAndProviderId(
                        userInfo.getProvider(), userInfo.getProviderId())
                .ifPresent(existing -> {
                    throw new CoreException(
                            ErrorType.SOCIAL_ACCOUNT_ALREADY_LINKED);
                });

        SocialAccount newAccount = SocialAccount.create(
                memberId,
                userInfo.getProvider(),
                userInfo.getProviderId(),
                userInfo.getEmail(),
                userInfo.getNickname(),
                userInfo.getProfileImageUrl());
        socialAccountPersistencePort.save(newAccount);
    }

    @Override
    @Transactional
    public void unlinkSocialAccount(Long memberId, Long socialAccountId) {
        SocialAccount account = socialAccountPersistencePort
                .findById(socialAccountId)
                .orElseThrow(() -> new CoreException(
                        ErrorType.SOCIAL_ACCOUNT_NOT_FOUND));

        if (!account.getMemberId().equals(memberId)) {
            throw new CoreException(ErrorType.FORBIDDEN);
        }

        socialAccountPersistencePort.delete(account);
    }

    @Override
    @Transactional
    public void logout(Long memberId) {
        refreshTokenPort.delete(memberId);
    }

    private AuthTokenReadModel findOrCreateMemberAndGenerateTokens(
            OAuthUserInfo userInfo) {
        SocialAccount socialAccount = socialAccountPersistencePort
                .findByProviderAndProviderId(
                        userInfo.getProvider(), userInfo.getProviderId())
                .orElse(null);

        Member member;
        if (socialAccount != null) {
            member = memberPersistencePort.findById(
                    socialAccount.getMemberId())
                    .orElseThrow(() -> new CoreException(
                            ErrorType.MEMBER_NOT_FOUND));
            member.updateLastLogin();
            memberPersistencePort.save(member);
        } else {
            member = Member.createNew();
            member = memberPersistencePort.save(member);

            SocialAccount newSocialAccount = SocialAccount.create(
                    member.getId(),
                    userInfo.getProvider(),
                    userInfo.getProviderId(),
                    userInfo.getEmail(),
                    userInfo.getNickname(),
                    userInfo.getProfileImageUrl());
            socialAccountPersistencePort.save(newSocialAccount);
        }

        return generateTokens(member);
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
