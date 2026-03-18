package com.example.lolserver.domain.member.application;

import com.example.lolserver.domain.member.application.dto.OAuthLoginCommand;
import com.example.lolserver.domain.member.application.dto.RiotLinkCommand;
import com.example.lolserver.domain.member.application.dto.TokenRefreshCommand;
import com.example.lolserver.domain.member.application.model.AuthTokenReadModel;
import com.example.lolserver.domain.member.application.model.MemberReadModel;
import com.example.lolserver.domain.member.application.model.OAuthUserInfo;
import com.example.lolserver.domain.member.application.model.RiotAccountLinkReadModel;
import com.example.lolserver.domain.member.application.port.in.MemberAuthUseCase;
import com.example.lolserver.domain.member.application.port.in.RiotAccountLinkUseCase;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService implements MemberAuthUseCase, RiotAccountLinkUseCase {

    private final MemberPersistencePort memberPersistencePort;
    private final RiotAccountLinkPersistencePort riotAccountLinkPersistencePort;
    private final OAuthClientPort oAuthClientPort;
    private final TokenPort tokenPort;
    private final RefreshTokenPort refreshTokenPort;
    private final OAuthStatePort oAuthStatePort;
    private final OAuthAuthorizationPort oAuthAuthorizationPort;

    @Override
    public String getOAuthAuthorizationUrl(OAuthProvider provider) {
        String state = UUID.randomUUID().toString();
        oAuthStatePort.saveState(state, 300);
        return oAuthAuthorizationPort.buildAuthorizationUrl(provider, state);
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
                : oAuthAuthorizationPort.getCallbackUri(command.getProvider());

        OAuthUserInfo userInfo = oAuthClientPort.getUserInfo(
                command.getProvider(), command.getCode(), redirectUri);

        Member member = memberPersistencePort
                .findByOAuthProviderAndProviderId(userInfo.getProvider(), userInfo.getProviderId())
                .orElse(null);

        if (member == null) {
            member = createMember(userInfo);
        } else {
            member.updateLastLogin();
            memberPersistencePort.save(member);
        }

        return generateTokens(member);
    }

    @Override
    @Transactional
    public AuthTokenReadModel refreshToken(TokenRefreshCommand command) {
        String refreshToken = command.getRefreshToken();

        if (!tokenPort.validateToken(refreshToken)) {
            throw new CoreException(ErrorType.INVALID_TOKEN);
        }

        Long memberId = tokenPort.getMemberIdFromToken(refreshToken);

        String savedToken = refreshTokenPort.find(memberId)
                .orElseThrow(() -> new CoreException(ErrorType.EXPIRED_TOKEN));

        if (!savedToken.equals(refreshToken)) {
            throw new CoreException(ErrorType.INVALID_TOKEN);
        }

        Member member = memberPersistencePort.findById(memberId)
                .orElseThrow(() -> new CoreException(ErrorType.MEMBER_NOT_FOUND));

        return generateTokens(member);
    }

    @Override
    @Transactional
    public void logout(Long memberId) {
        refreshTokenPort.delete(memberId);
    }

    @Override
    @Transactional
    public RiotAccountLinkReadModel linkRiotAccount(Long memberId, RiotLinkCommand command) {
        OAuthUserInfo riotInfo = oAuthClientPort.getUserInfo(
                OAuthProvider.RIOT, command.getCode(), command.getRedirectUri());

        riotAccountLinkPersistencePort.findByMemberIdAndPuuid(memberId, riotInfo.getPuuid())
                .ifPresent(existing -> {
                    throw new CoreException(ErrorType.RIOT_ACCOUNT_ALREADY_LINKED);
                });

        RiotAccountLink link = new RiotAccountLink();
        link.setMemberId(memberId);
        link.setPuuid(riotInfo.getPuuid());
        link.setGameName(riotInfo.getGameName());
        link.setTagLine(riotInfo.getTagLine());
        link.setPlatformId(command.getPlatformId());
        link.setLinkedAt(LocalDateTime.now());

        RiotAccountLink saved = riotAccountLinkPersistencePort.save(link);
        return RiotAccountLinkReadModel.of(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RiotAccountLinkReadModel> getLinkedAccounts(Long memberId) {
        return riotAccountLinkPersistencePort.findByMemberId(memberId).stream()
                .map(RiotAccountLinkReadModel::of)
                .toList();
    }

    @Override
    @Transactional
    public void unlinkRiotAccount(Long memberId, Long linkId) {
        RiotAccountLink link = riotAccountLinkPersistencePort
                .findByIdAndMemberId(linkId, memberId)
                .orElseThrow(() -> new CoreException(ErrorType.RIOT_LINK_NOT_FOUND));

        riotAccountLinkPersistencePort.delete(link);
    }

    @Override
    @Transactional(readOnly = true)
    public MemberReadModel getMyProfile(Long memberId) {
        Member member = memberPersistencePort.findById(memberId)
                .orElseThrow(() -> new CoreException(ErrorType.MEMBER_NOT_FOUND));

        return MemberReadModel.of(member);
    }

    private Member createMember(OAuthUserInfo userInfo) {
        Member member = new Member();
        member.setEmail(userInfo.getEmail());
        member.setNickname(userInfo.getNickname());
        member.setProfileImageUrl(userInfo.getProfileImageUrl());
        member.setOauthProvider(userInfo.getProvider());
        member.setOauthProviderId(userInfo.getProviderId());
        member.setRole("USER");
        member.setCreatedAt(LocalDateTime.now());
        member.setLastLoginAt(LocalDateTime.now());
        return memberPersistencePort.save(member);
    }

    private AuthTokenReadModel generateTokens(Member member) {
        String accessToken = tokenPort.generateAccessToken(member.getId(), member.getRole());
        String refreshToken = tokenPort.generateRefreshToken(member.getId(), member.getRole());

        refreshTokenPort.save(member.getId(), refreshToken, tokenPort.getRefreshTokenExpiry());

        return new AuthTokenReadModel(accessToken, refreshToken, tokenPort.getAccessTokenExpiry());
    }
}
