package com.example.lolserver.duo.application;

import com.example.lolserver.duo.application.port.out.SummonerStatsPort;
import com.example.lolserver.duo.domain.vo.MostChampion;
import com.example.lolserver.duo.domain.vo.RecentGameSummary;
import com.example.lolserver.duo.domain.vo.RiotAccountStats;
import com.example.lolserver.duo.domain.vo.SummonerStats;
import com.example.lolserver.duo.domain.vo.TierInfo;
import com.example.lolserver.member.application.model.readmodel.LinkedRiotAccountReadModel;
import com.example.lolserver.member.application.port.in.MemberQueryUseCase;
import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class RiotAccountResolverTest {

    @InjectMocks
    private RiotAccountResolver riotAccountResolver;

    @Mock
    private MemberQueryUseCase memberQueryUseCase;

    @Mock
    private SummonerStatsPort summonerStatsPort;

    private static LinkedRiotAccountReadModel linked(String gameName, String tagLine) {
        return LinkedRiotAccountReadModel.builder()
                .puuid("test-puuid").gameName(gameName).tagLine(tagLine).build();
    }

    @Nested
    @DisplayName("resolveLinkedAccount")
    class ResolveLinkedAccount {

        @DisplayName("회원이 존재하지 않으면 MEMBER_NOT_FOUND 에러")
        @Test
        void memberNotFound_throwsException() {
            // given
            Long memberId = 1L;
            given(memberQueryUseCase.findLinkedRiotAccount(memberId))
                    .willThrow(new CoreException(ErrorType.MEMBER_NOT_FOUND));

            // when & then
            assertThatThrownBy(() -> riotAccountResolver.resolveLinkedAccount(memberId))
                    .isInstanceOf(CoreException.class)
                    .extracting(e -> ((CoreException) e).getErrorType())
                    .isEqualTo(ErrorType.MEMBER_NOT_FOUND);
        }

        @DisplayName("Riot 계정 미연동 시 RIOT_ACCOUNT_NOT_LINKED 에러")
        @Test
        void riotNotLinked_throwsException() {
            // given
            Long memberId = 1L;
            given(memberQueryUseCase.findLinkedRiotAccount(memberId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> riotAccountResolver.resolveLinkedAccount(memberId))
                    .isInstanceOf(CoreException.class)
                    .extracting(e -> ((CoreException) e).getErrorType())
                    .isEqualTo(ErrorType.RIOT_ACCOUNT_NOT_LINKED);
        }

        @DisplayName("Riot 계정 연동 시 puuid·연동 시점 Riot ID 반환")
        @Test
        void success() {
            // given
            Long memberId = 1L;
            given(memberQueryUseCase.findLinkedRiotAccount(memberId))
                    .willReturn(Optional.of(linked("hide on bush", "KR1")));

            // when
            LinkedRiotAccountReadModel account = riotAccountResolver.resolveLinkedAccount(memberId);

            // then
            assertThat(account.getPuuid()).isEqualTo("test-puuid");
            assertThat(account.getGameName()).isEqualTo("hide on bush");
        }
    }

    @Nested
    @DisplayName("lookupStats")
    class LookupStats {

        @DisplayName("lol-repository 통계에 연동 때 받은 Riot ID 를 붙인다")
        @Test
        void indexed_returnsStatsWithLinkedRiotId() {
            // given
            SummonerStats stats = new SummonerStats(
                    new TierInfo("GOLD", "II", 50),
                    List.of(new MostChampion(1, "Annie", 10, 6, 4)),
                    new RecentGameSummary(1, 0,
                            List.of(new RecentGameSummary.PlayedChampion(1, "Annie"))));
            given(summonerStatsPort.findByPuuid("test-puuid")).willReturn(Optional.of(stats));

            // when
            RiotAccountStats result = riotAccountResolver.lookupStats(linked("hide on bush", "KR1"));

            // then
            assertThat(result).isEqualTo(new RiotAccountStats(
                    "hide on bush", "KR1",
                    stats.tierInfo(), stats.mostChampions(), stats.recentGameSummary()));
        }

        @DisplayName("통계를 받지 못하면(무응답·장애·색인 없음) 통계 없이 연동 Riot ID 만 반환한다")
        @Test
        void statsUnavailable_returnsUnknownWithLinkedRiotId() {
            // given
            given(summonerStatsPort.findByPuuid("test-puuid")).willReturn(Optional.empty());

            // when
            RiotAccountStats result = riotAccountResolver.lookupStats(linked("hide on bush", "KR1"));

            // then
            assertThat(result.isKnown()).isFalse();
            assertThat(result.tierOrUnranked()).isEqualTo(TierInfo.UNRANKED);
            assertThat(result.gameName()).isEqualTo("hide on bush");
            assertThat(result.tagLine()).isEqualTo("KR1");
            assertThat(result.mostChampions()).isEmpty();
            assertThat(result.recentGameSummary()).isNull();
        }

        @DisplayName("연동에 Riot ID 가 없으면(V4 이전 연동) lol-repository 를 부르지 않고 RIOT_ACCOUNT_RELINK_REQUIRED 에러")
        @ParameterizedTest(name = "gameName={0}, tagLine={1}")
        @CsvSource(value = {"NULL, NULL", "hide on bush, NULL", "NULL, KR1", "' ', KR1"}, nullValues = "NULL")
        void linkedWithoutRiotId_throwsRelinkRequired(String gameName, String tagLine) {
            // when & then
            assertThatThrownBy(() -> riotAccountResolver.lookupStats(linked(gameName, tagLine)))
                    .isInstanceOf(CoreException.class)
                    .extracting(e -> ((CoreException) e).getErrorType())
                    .isEqualTo(ErrorType.RIOT_ACCOUNT_RELINK_REQUIRED);
            verifyNoInteractions(summonerStatsPort);
        }
    }
}
