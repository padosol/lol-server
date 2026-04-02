package com.example.lolserver.domain.member.application;

import com.example.lolserver.domain.member.application.dto.RiotLinkCommand;
import com.example.lolserver.domain.member.application.model.OAuthUserInfo;
import com.example.lolserver.domain.member.application.model.RiotAccountLinkReadModel;
import com.example.lolserver.domain.member.application.port.in.RiotAccountLinkUseCase;
import com.example.lolserver.domain.member.application.port.out.OAuthClientPort;
import com.example.lolserver.domain.member.application.port.out.RiotAccountLinkPersistencePort;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class RiotAccountLinkService implements RiotAccountLinkUseCase {

    private final RiotAccountLinkPersistencePort
            riotAccountLinkPersistencePort;
    private final OAuthClientPort oAuthClientPort;

    @Override
    @Transactional
    public RiotAccountLinkReadModel linkRiotAccount(
            Long memberId, RiotLinkCommand command) {
        OAuthUserInfo riotInfo = oAuthClientPort.getUserInfo(
                OAuthProvider.RIOT, command.getCode(),
                command.getRedirectUri());

        riotAccountLinkPersistencePort
                .findByMemberIdAndPuuid(memberId, riotInfo.getPuuid())
                .ifPresent(existing -> {
                    throw new CoreException(
                            ErrorType.RIOT_ACCOUNT_ALREADY_LINKED);
                });

        RiotAccountLink link = RiotAccountLink.builder()
                .memberId(memberId)
                .puuid(riotInfo.getPuuid())
                .gameName(riotInfo.getGameName())
                .tagLine(riotInfo.getTagLine())
                .platformId(command.getPlatformId())
                .linkedAt(LocalDateTime.now())
                .build();

        RiotAccountLink saved =
                riotAccountLinkPersistencePort.save(link);
        return RiotAccountLinkReadModel.of(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RiotAccountLinkReadModel> getLinkedAccounts(
            Long memberId) {
        return riotAccountLinkPersistencePort.findByMemberId(memberId)
                .stream()
                .map(RiotAccountLinkReadModel::of)
                .toList();
    }

    @Override
    @Transactional
    public void unlinkRiotAccount(Long memberId, Long linkId) {
        RiotAccountLink link = riotAccountLinkPersistencePort
                .findByIdAndMemberId(linkId, memberId)
                .orElseThrow(() -> new CoreException(
                        ErrorType.RIOT_LINK_NOT_FOUND));

        riotAccountLinkPersistencePort.delete(link);
    }
}
