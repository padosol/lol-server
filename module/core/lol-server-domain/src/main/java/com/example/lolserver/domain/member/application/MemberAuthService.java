package com.example.lolserver.domain.member.application;

import com.example.lolserver.domain.member.application.dto.OAuthLoginCommand;
import com.example.lolserver.domain.member.application.dto.TokenRefreshCommand;
import com.example.lolserver.domain.member.application.model.AuthTokenReadModel;
import com.example.lolserver.domain.member.application.model.OAuthUserInfo;
import com.example.lolserver.domain.member.application.port.in.MemberAuthUseCase;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberAuthService implements MemberAuthUseCase {

    private final MemberPersistencePort memberPersistencePort;
    private final SocialAccountPersistencePort socialAccountPersistencePort;
    private final MemberWithdrawalPersistencePort memberWithdrawalPersistencePort;
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

        if (member.isWithdrawn()) {
            throw new CoreException(ErrorType.MEMBER_WITHDRAWN);
        }

        return generateTokens(member);
    }

    @Override
    @Transactional
    public void linkSocialAccount(Long memberId, OAuthUserInfo userInfo) {
        socialAccountPersistencePort
                .findByProviderAndProviderId(
                        userInfo.getProvider(), userInfo.getProviderId())
                .ifPresent(existing -> {
                    throw new CoreException(
                            ErrorType.SOCIAL_ACCOUNT_ALREADY_LINKED);
                });

        Member member = findMemberWithSocialAccounts(memberId);

        member.linkSocialAccount(
                userInfo.getProvider(), userInfo.getProviderId(),
                userInfo.getEmail(), userInfo.getNickname(),
                userInfo.getProfileImageUrl());

        memberPersistencePort.save(member);
    }

    @Override
    @Transactional
    public void unlinkSocialAccount(Long memberId, Long socialAccountId) {
        Member member = findMemberWithSocialAccounts(memberId);
        member.unlinkSocialAccount(socialAccountId);
        memberPersistencePort.save(member);
    }

    @Override
    @Transactional
    public void logout(Long memberId) {
        refreshTokenPort.delete(memberId);
    }

    @Override
    @Transactional
    public void withdraw(Long memberId) {
        Member member = findMemberWithSocialAccounts(memberId);

        List<MemberWithdrawal> withdrawals =
                member.getSocialAccounts().stream()
                        .map(sa -> MemberWithdrawal.create(
                                sa.getProvider(),
                                sa.getProviderId()))
                        .toList();

        member.withdraw();
        memberPersistencePort.save(member);

        withdrawals.forEach(
                memberWithdrawalPersistencePort::save);

        refreshTokenPort.delete(memberId);
    }

    private AuthTokenReadModel findOrCreateMemberAndGenerateTokens(
            OAuthUserInfo userInfo) {
        SocialAccount socialAccount = socialAccountPersistencePort
                .findByProviderAndProviderId(
                        userInfo.getProvider(), userInfo.getProviderId())
                .orElse(null);

        Member member = socialAccount != null
                ? loginExistingMember(socialAccount)
                : registerNewMember(userInfo);

        return generateTokens(member);
    }

    private Member loginExistingMember(SocialAccount socialAccount) {
        Member member = memberPersistencePort.findById(
                socialAccount.getMemberId())
                .orElseThrow(() -> new CoreException(
                        ErrorType.MEMBER_NOT_FOUND));
        if (member.isWithdrawn()) {
            throw new CoreException(ErrorType.MEMBER_WITHDRAWN);
        }
        member.updateLastLogin();
        memberPersistencePort.save(member);
        return member;
    }

    private Member registerNewMember(OAuthUserInfo userInfo) {
        memberWithdrawalPersistencePort
                .findByProviderAndProviderId(
                        userInfo.getProvider(),
                        userInfo.getProviderId())
                .filter(MemberWithdrawal
                        ::isWithinRestrictionPeriod)
                .ifPresent(w -> {
                    throw new CoreException(
                            ErrorType.WITHDRAWAL_REREGISTRATION_RESTRICTED);
                });

        Member member = Member.createNewWithSocialAccount(
                userInfo.getProvider(),
                userInfo.getProviderId(),
                userInfo.getEmail(),
                userInfo.getNickname(),
                userInfo.getProfileImageUrl());
        return memberPersistencePort.save(member);
    }

    private Member findMemberWithSocialAccounts(Long memberId) {
        Member member = memberPersistencePort
                .findByIdWithSocialAccounts(memberId)
                .orElseThrow(() -> new CoreException(
                        ErrorType.MEMBER_NOT_FOUND));
        if (member.isWithdrawn()) {
            throw new CoreException(ErrorType.MEMBER_WITHDRAWN);
        }
        return member;
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
