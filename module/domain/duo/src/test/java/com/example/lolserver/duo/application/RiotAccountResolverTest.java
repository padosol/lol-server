package com.example.lolserver.duo.application;

import com.example.lolserver.duo.application.port.out.RiotAccountStatsPort;
import com.example.lolserver.duo.domain.vo.MostChampion;
import com.example.lolserver.duo.domain.vo.RecentGameSummary;
import com.example.lolserver.duo.domain.vo.RiotAccountStats;
import com.example.lolserver.duo.domain.vo.TierInfo;
import com.example.lolserver.member.application.port.in.MemberQueryUseCase;
import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class RiotAccountResolverTest {

    @InjectMocks
    private RiotAccountResolver riotAccountResolver;

    @Mock
    private MemberQueryUseCase memberQueryUseCase;

    @Mock
    private RiotAccountStatsPort riotAccountStatsPort;

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
    @DisplayName("lookupAllStats")
    class LookupAllStats {

        @DisplayName("전적 색인이 없는 계정은 언랭이 아니라 SUMMONER_SEARCH_REQUIRED 로 구분한다")
        @Test
        void summonerNotIndexed_throwsSearchRequired() {
            // given
            String puuid = "test-puuid";
            given(riotAccountStatsPort.findByPuuid(puuid)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> riotAccountResolver.lookupAllStats(puuid))
                    .isInstanceOf(CoreException.class)
                    .extracting(e -> ((CoreException) e).getErrorType())
                    .isEqualTo(ErrorType.SUMMONER_SEARCH_REQUIRED);
        }

        @DisplayName("lol-repository 가 준 Riot ID·티어·전적 요약을 그대로 반환한다")
        @Test
        void indexed_returnsStats() {
            // given
            String puuid = "test-puuid";
            RiotAccountStats stats = new RiotAccountStats(
                    "hide on bush", "KR1",
                    new TierInfo("GOLD", "II", 50),
                    List.of(new MostChampion(1, "Annie", 10, 6, 4)),
                    new RecentGameSummary(1, 0,
                            List.of(new RecentGameSummary.PlayedChampion(1, "Annie"))));
            given(riotAccountStatsPort.findByPuuid(puuid)).willReturn(Optional.of(stats));

            // when
            RiotAccountStats result = riotAccountResolver.lookupAllStats(puuid);

            // then
            assertThat(result).isEqualTo(stats);
        }

        @DisplayName("lol-repository 장애(RIOT_ACCOUNT_STATS_UNAVAILABLE)는 그대로 전파한다")
        @Test
        void upstreamFailure_propagatesCoreException() {
            // given
            String puuid = "test-puuid";
            given(riotAccountStatsPort.findByPuuid(puuid))
                    .willThrow(new CoreException(ErrorType.RIOT_ACCOUNT_STATS_UNAVAILABLE));

            // when & then
            assertThatThrownBy(() -> riotAccountResolver.lookupAllStats(puuid))
                    .isInstanceOf(CoreException.class)
                    .extracting(e -> ((CoreException) e).getErrorType())
                    .isEqualTo(ErrorType.RIOT_ACCOUNT_STATS_UNAVAILABLE);
        }
    }
}
