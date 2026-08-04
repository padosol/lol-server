package com.example.lolserver.duo.application;

import com.example.lolserver.duo.domain.vo.MostChampion;
import com.example.lolserver.duo.domain.vo.RecentGameSummary;
import com.example.lolserver.duo.domain.vo.RiotAccountStats;
import com.example.lolserver.duo.domain.vo.TierInfo;
import com.example.lolserver.match.application.command.MSChampionCommand;
import com.example.lolserver.match.application.model.readmodel.MSChampionReadModel;
import com.example.lolserver.match.application.model.readmodel.PlayerMatchReadModel;
import com.example.lolserver.match.application.port.in.MatchQueryUseCase;
import com.example.lolserver.member.application.port.in.MemberQueryUseCase;
import com.example.lolserver.summoner.application.model.readmodel.LeagueReadModel;
import com.example.lolserver.summoner.application.port.in.LeagueQueryUseCase;
import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class RiotAccountResolverTest {

    @InjectMocks
    private RiotAccountResolver riotAccountResolver;

    @Mock
    private MemberQueryUseCase memberQueryUseCase;

    @Mock
    private LeagueQueryUseCase leagueQueryUseCase;

    @Mock
    private MatchQueryUseCase matchQueryUseCase;

    @Nested
    @DisplayName("extractRiotPuuid")
    class ExtractRiotPuuid {

        @DisplayName("회원이 존재하지 않으면 MEMBER_NOT_FOUND 에러")
        @Test
        void memberNotFound_throwsException() {
            // given
            Long memberId = 1L;
            given(memberQueryUseCase.findRiotPuuid(memberId))
                    .willThrow(new CoreException(ErrorType.MEMBER_NOT_FOUND));

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
            given(memberQueryUseCase.findRiotPuuid(memberId))
                    .willReturn(Optional.empty());

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
            given(memberQueryUseCase.findRiotPuuid(memberId))
                    .willReturn(Optional.of("test-puuid"));

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
            given(leagueQueryUseCase.getLeagueSummariesByPuuid(puuid))
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
            LeagueReadModel league = LeagueReadModel.builder()
                    .queue("RANKED_SOLO_5x5")
                    .tier("GOLD")
                    .rank("I")
                    .leaguePoints(50)
                    .build();
            given(leagueQueryUseCase.getLeagueSummariesByPuuid(puuid))
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
            List<MSChampionReadModel> champions = List.of(
                    MSChampionReadModel.builder().championId(1).championName("Ahri").playCount(100L).win(60L).losses(40L).build(),
                    MSChampionReadModel.builder().championId(2).championName("Zed").playCount(80L).win(45L).losses(35L).build(),
                    MSChampionReadModel.builder().championId(3).championName("Lux").playCount(50L).win(30L).losses(20L).build(),
                    MSChampionReadModel.builder().championId(4).championName("Yasuo").playCount(30L).win(10L).losses(20L).build()
            );
            given(matchQueryUseCase.getRankChampionSummaries(any(MSChampionCommand.class)))
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
            given(matchQueryUseCase.getRankChampionSummaries(any(MSChampionCommand.class)))
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
            List<PlayerMatchReadModel> matches = List.of(
                    PlayerMatchReadModel.builder().win(true).championId(1).championName("Ahri").build(),
                    PlayerMatchReadModel.builder().win(false).championId(2).championName("Zed").build()
            );
            given(matchQueryUseCase.getRecentPlayerMatches(eq(puuid), eq(420), anyInt()))
                    .willReturn(matches);

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
            given(matchQueryUseCase.getRecentPlayerMatches(eq(puuid), eq(420), anyInt()))
                    .willReturn(Collections.emptyList());

            // when
            RecentGameSummary result = riotAccountResolver.lookupRecentGameSummary(puuid);

            // then
            assertThat(result.wins()).isEqualTo(0);
            assertThat(result.losses()).isEqualTo(0);
            assertThat(result.playedChampions()).isEmpty();
        }
    }

    @Nested
    @DisplayName("lookupAllStats")
    @MockitoSettings(strictness = Strictness.LENIENT)
    class LookupAllStats {

        @DisplayName("랭크 정보 없어도 tier/rank 는 null 이 아니다 - NOT NULL 컬럼 저장 가능")
        @Test
        void noRankedData_returnsNonNullTierMarker() {
            // given
            String puuid = "test-puuid";
            given(leagueQueryUseCase.getLeagueSummariesByPuuid(puuid))
                    .willReturn(Collections.emptyList());
            given(matchQueryUseCase.getRankChampionSummaries(any(MSChampionCommand.class)))
                    .willReturn(Collections.emptyList());
            given(matchQueryUseCase.getRecentPlayerMatches(eq(puuid), eq(420), anyInt()))
                    .willReturn(Collections.emptyList());

            // when
            RiotAccountStats stats = riotAccountResolver.lookupAllStats(puuid);

            // then
            assertThat(stats.tierInfo().tier()).isNotNull();
            assertThat(stats.tierInfo().rank()).isNotNull();
            assertThat(stats.tierInfo()).isEqualTo(TierInfo.UNRANKED);
        }

        @DisplayName("상위 조회 예외는 CompletionException 으로 감싸지 않고 그대로 전파한다")
        @Test
        void upstreamFailure_propagatesCoreException() {
            // given
            String puuid = "test-puuid";
            given(leagueQueryUseCase.getLeagueSummariesByPuuid(puuid))
                    .willThrow(new CoreException(ErrorType.MEMBER_NOT_FOUND));
            given(matchQueryUseCase.getRankChampionSummaries(any(MSChampionCommand.class)))
                    .willReturn(Collections.emptyList());
            given(matchQueryUseCase.getRecentPlayerMatches(eq(puuid), eq(420), anyInt()))
                    .willReturn(Collections.emptyList());

            // when & then
            assertThatThrownBy(() -> riotAccountResolver.lookupAllStats(puuid))
                    .isInstanceOf(CoreException.class)
                    .extracting(e -> ((CoreException) e).getErrorType())
                    .isEqualTo(ErrorType.MEMBER_NOT_FOUND);
        }
    }
}
