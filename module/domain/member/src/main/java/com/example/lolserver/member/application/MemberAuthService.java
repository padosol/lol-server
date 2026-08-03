package com.example.lolserver.member.application;

import com.example.lolserver.member.application.dto.TokenRefreshCommand;
import com.example.lolserver.member.application.model.resultmodel.AuthTokenResultModel;
import com.example.lolserver.member.application.model.OAuthUserInfo;
import com.example.lolserver.member.application.port.in.MemberAuthUseCase;
import com.example.lolserver.member.application.port.out.MemberPersistencePort;
import com.example.lolserver.member.application.port.out.MemberWithdrawalPersistencePort;
import com.example.lolserver.member.application.port.out.RefreshTokenPort;
import com.example.lolserver.member.application.port.out.SocialAccountPersistencePort;
import com.example.lolserver.member.application.port.out.TokenPort;
import com.example.lolserver.member.domain.Member;
import com.example.lolserver.member.domain.MemberWithdrawal;
import com.example.lolserver.member.domain.SocialAccount;
import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberAuthService implements MemberAuthUseCase {

    private final MemberPersistencePort memberPersistencePort;
    private final SocialAccountPersistencePort socialAccountPersistencePort;
    private final MemberWithdrawalPersistencePort memberWithdrawalPersistencePort;
    private final TokenPort tokenPort;
    private final RefreshTokenPort refreshTokenPort;

    @Override
    @Transactional
    public AuthTokenResultModel loginWithOAuthUserInfo(
            OAuthUserInfo userInfo) {
        return findOrCreateMemberAndGenerateTokens(userInfo);
    }

    @Override
    @Transactional
    public AuthTokenResultModel refreshToken(
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

        member.validateNotWithdrawn();

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
                userInfo.getProfileImageUrl(), userInfo.getPuuid());

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

    private AuthTokenResultModel findOrCreateMemberAndGenerateTokens(
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
        member.validateNotWithdrawn();
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
                userInfo.getProfileImageUrl(),
                userInfo.getPuuid());
        return memberPersistencePort.save(member);
    }

    private Member findMemberWithSocialAccounts(Long memberId) {
        Member member = memberPersistencePort
                .findByIdWithSocialAccounts(memberId)
                .orElseThrow(() -> new CoreException(
                        ErrorType.MEMBER_NOT_FOUND));
        member.validateNotWithdrawn();
        return member;
    }

    private AuthTokenResultModel generateTokens(Member member) {
        String accessToken = tokenPort.generateAccessToken(
                member.getId(), member.getRole());
        String refreshToken = tokenPort.generateRefreshToken(
                member.getId(), member.getRole());

        refreshTokenPort.save(member.getId(), refreshToken,
                tokenPort.getRefreshTokenExpiry());

        return new AuthTokenResultModel(accessToken, refreshToken,
                tokenPort.getAccessTokenExpiry());
    }
}
