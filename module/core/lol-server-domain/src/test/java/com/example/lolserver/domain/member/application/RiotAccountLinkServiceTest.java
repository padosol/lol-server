package com.example.lolserver.domain.member.application;

import com.example.lolserver.domain.member.application.dto.RiotLinkCommand;
import com.example.lolserver.domain.member.application.model.OAuthUserInfo;
import com.example.lolserver.domain.member.application.model.RiotAccountLinkReadModel;
import com.example.lolserver.domain.member.application.port.out.OAuthClientPort;
import com.example.lolserver.domain.member.application.port.out.RiotAccountLinkPersistencePort;
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

@ExtendWith(MockitoExtension.class)
class RiotAccountLinkServiceTest {

    @Mock
    private RiotAccountLinkPersistencePort riotAccountLinkPersistencePort;

    @Mock
    private OAuthClientPort oAuthClientPort;

    @InjectMocks
    private RiotAccountLinkService riotAccountLinkService;

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
        RiotAccountLinkReadModel result = riotAccountLinkService.linkRiotAccount(memberId, command);

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
        assertThatThrownBy(() -> riotAccountLinkService.linkRiotAccount(memberId, command))
                .isInstanceOf(CoreException.class)
                .extracting(e -> ((CoreException) e).getErrorType())
                .isEqualTo(ErrorType.RIOT_ACCOUNT_ALREADY_LINKED);
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
        List<RiotAccountLinkReadModel> result = riotAccountLinkService.getLinkedAccounts(memberId);

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
        riotAccountLinkService.unlinkRiotAccount(memberId, linkId);

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
        assertThatThrownBy(() -> riotAccountLinkService.unlinkRiotAccount(memberId, linkId))
                .isInstanceOf(CoreException.class)
                .extracting(e -> ((CoreException) e).getErrorType())
                .isEqualTo(ErrorType.RIOT_LINK_NOT_FOUND);
    }
}
