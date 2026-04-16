package com.example.lolserver.domain.duo.application;

import com.example.lolserver.domain.duo.domain.vo.MostChampion;
import com.example.lolserver.domain.duo.domain.vo.RecentGameSummary;
import com.example.lolserver.domain.duo.domain.vo.TierInfo;
import com.example.lolserver.domain.league.application.port.LeaguePersistencePort;
import com.example.lolserver.domain.league.domain.League;
import com.example.lolserver.domain.match.application.model.GameReadModel;
import com.example.lolserver.domain.match.application.port.out.MatchPersistencePort;
import com.example.lolserver.domain.match.domain.MSChampion;
import com.example.lolserver.domain.match.domain.gamedata.ParticipantData;
import com.example.lolserver.domain.member.application.port.out.MemberPersistencePort;
import com.example.lolserver.domain.member.domain.Member;
import com.example.lolserver.domain.member.domain.SocialAccount;
import com.example.lolserver.support.PaginationRequest;
import com.example.lolserver.support.SliceResult;
import com.example.lolserver.support.error.CoreException;
import com.example.lolserver.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class RiotAccountResolverTest {

    @InjectMocks
    private RiotAccountResolver riotAccountResolver;

    @Mock
    private MemberPersistencePort memberPersistencePort;

    @Mock
    private LeaguePersistencePort leaguePersistencePort;

    @Mock
    private MatchPersistencePort matchPersistencePort;

    @Nested
    @DisplayName("extractRiotPuuid")
    class ExtractRiotPuuid {

        @DisplayName("회원이 존재하지 않으면 MEMBER_NOT_FOUND 에러")
        @Test
        void memberNotFound_throwsException() {
            // given
            Long memberId = 1L;
            given(memberPersistencePort.findByIdWithSocialAccounts(memberId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> riotAccountResolver.extractRiotPuuid(memberId))
                    .isInstanceOf(CoreException.class)
                    .extracting(e -> ((CoreException) e).getErrorType())
                    .isEqualTo(ErrorType.MEMBER_NOT_FOUND);
        }

        @DisplayName("Riot 계정 미연동 시 RIOT_ACCOUNT_NOT_LINKED 에러")
        @Test
        void riotNotLinked_throwsException() {
            // given
            Long memberId = 1L;
            Member member = createTestMember(memberId, false);
            given(memberPersistencePort.findByIdWithSocialAccounts(memberId))
                    .willReturn(Optional.of(member));

            // when & then
            assertThatThrownBy(() -> riotAccountResolver.extractRiotPuuid(memberId))
                    .isInstanceOf(CoreException.class)
                    .extracting(e -> ((CoreException) e).getErrorType())
                    .isEqualTo(ErrorType.RIOT_ACCOUNT_NOT_LINKED);
        }

        @DisplayName("Riot 계정 연동 시 PUUID 반환")
        @Test
        void success() {
            // given
            Long memberId = 1L;
            Member member = createTestMember(memberId, true);
            given(memberPersistencePort.findByIdWithSocialAccounts(memberId))
                    .willReturn(Optional.of(member));

            // when
            String puuid = riotAccountResolver.extractRiotPuuid(memberId);

            // then
            assertThat(puuid).isEqualTo("test-puuid");
        }
    }

    @Nested
    @DisplayName("lookupTierInfo")
    class LookupTierInfo {

        @DisplayName("랭크 정보 없으면 UNRANKED 반환")
        @Test
        void noRankedData_returnsUnranked() {
            // given
            String puuid = "test-puuid";
            given(leaguePersistencePort.findAllLeaguesByPuuid(puuid))
                    .willReturn(Collections.emptyList());

            // when
            TierInfo tierInfo = riotAccountResolver.lookupTierInfo(puuid);

            // then
            assertThat(tierInfo).isEqualTo(TierInfo.UNRANKED);
        }

        @DisplayName("랭크 정보 있으면 TierInfo 반환")
        @Test
        void withRankedData_returnsTierInfo() {
            // given
            String puuid = "test-puuid";
            League league = League.builder()
                    .id(1L)
                    .puuid(puuid)
                    .queue("RANKED_SOLO_5x5")
                    .tier("GOLD")
                    .rank("I")
                    .leaguePoints(50)
                    .build();
            given(leaguePersistencePort.findAllLeaguesByPuuid(puuid))
                    .willReturn(List.of(league));

            // when
            TierInfo tierInfo = riotAccountResolver.lookupTierInfo(puuid);

            // then
            assertThat(tierInfo.tier()).isEqualTo("GOLD");
            assertThat(tierInfo.rank()).isEqualTo("I");
            assertThat(tierInfo.leaguePoints()).isEqualTo(50);
        }
    }

    @Nested
    @DisplayName("lookupMostChampions")
    class LookupMostChampions {

        @DisplayName("모스트 챔피언 상위 3개 반환")
        @Test
        void returnsTop3Champions() {
            // given
            String puuid = "test-puuid";
            List<MSChampion> champions = List.of(
                    MSChampion.builder().championId(1).championName("Ahri").playCount(100L).win(60L).losses(40L).build(),
                    MSChampion.builder().championId(2).championName("Zed").playCount(80L).win(45L).losses(35L).build(),
                    MSChampion.builder().championId(3).championName("Lux").playCount(50L).win(30L).losses(20L).build(),
                    MSChampion.builder().championId(4).championName("Yasuo").playCount(30L).win(10L).losses(20L).build()
            );
            given(matchPersistencePort.getRankChampions(puuid, null, 420))
                    .willReturn(champions);

            // when
            List<MostChampion> result = riotAccountResolver.lookupMostChampions(puuid);

            // then
            assertThat(result).hasSize(3);
            assertThat(result.get(0).championName()).isEqualTo("Ahri");
            assertThat(result.get(0).playCount()).isEqualTo(100L);
            assertThat(result.get(2).championName()).isEqualTo("Lux");
        }

        @DisplayName("데이터 없으면 빈 리스트 반환")
        @Test
        void noData_returnsEmptyList() {
            // given
            String puuid = "test-puuid";
            given(matchPersistencePort.getRankChampions(puuid, null, 420))
                    .willReturn(Collections.emptyList());

            // when
            List<MostChampion> result = riotAccountResolver.lookupMostChampions(puuid);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("lookupRecentGameSummary")
    class LookupRecentGameSummary {

        @DisplayName("최근 게임 승패 및 플레이챔피언 반환")
        @Test
        void returnsWinLossAndChampions() {
            // given
            String puuid = "test-puuid";
            GameReadModel game1 = createTestGame(puuid, true, 1, "Ahri");
            GameReadModel game2 = createTestGame(puuid, false, 2, "Zed");

            given(matchPersistencePort.getMatches(
                    eq(puuid),
                    eq(420),
                    any(PaginationRequest.class)))
                    .willReturn(new SliceResult<>(List.of(game1, game2), false));

            // when
            RecentGameSummary result = riotAccountResolver.lookupRecentGameSummary(puuid);

            // then
            assertThat(result.wins()).isEqualTo(1);
            assertThat(result.losses()).isEqualTo(1);
            assertThat(result.playedChampions()).hasSize(2);
            assertThat(result.playedChampions().get(0).championName()).isEqualTo("Ahri");
        }

        @DisplayName("데이터 없으면 빈 결과 반환")
        @Test
        void noData_returnsEmpty() {
            // given
            String puuid = "test-puuid";
            given(matchPersistencePort.getMatches(
                    eq(puuid),
                    eq(420),
                    any(PaginationRequest.class)))
                    .willReturn(new SliceResult<>(Collections.emptyList(), false));

            // when
            RecentGameSummary result = riotAccountResolver.lookupRecentGameSummary(puuid);

            // then
            assertThat(result.wins()).isEqualTo(0);
            assertThat(result.losses()).isEqualTo(0);
            assertThat(result.playedChampions()).isEmpty();
        }
    }

    private GameReadModel createTestGame(String puuid, boolean win, int championId, String championName) {
        ParticipantData participant = ParticipantData.builder()
                .puuid(puuid)
                .win(win)
                .championId(championId)
                .championName(championName)
                .build();
        GameReadModel game = new GameReadModel();
        game.setParticipantData(List.of(participant));
        return game;
    }

    private Member createTestMember(Long id, boolean hasRiot) {
        List<SocialAccount> socialAccounts = new ArrayList<>();
        if (hasRiot) {
            socialAccounts.add(SocialAccount.builder()
                    .id(1L)
                    .memberId(id)
                    .provider("RIOT")
                    .providerId("riot-provider-id")
                    .email("test@riot.com")
                    .nickname("라이엇유저")
                    .puuid("test-puuid")
                    .linkedAt(LocalDateTime.now())
                    .build());
        }
        return Member.builder()
                .id(id)
                .uuid("test-uuid-" + id)
                .nickname("테스터" + id)
                .role("USER")
                .socialAccounts(socialAccounts)
                .createdAt(LocalDateTime.now())
                .lastLoginAt(LocalDateTime.now())
                .build();
    }
}
