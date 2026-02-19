package com.example.lolserver.domain.summoner.application;

import com.example.lolserver.RenewalStatus;
import com.example.lolserver.domain.summoner.application.dto.SummonerAutoResponse;
import com.example.lolserver.domain.summoner.application.dto.SummonerRenewalInfoResponse;
import com.example.lolserver.domain.summoner.application.dto.SummonerResponse;
import com.example.lolserver.domain.summoner.application.port.out.SummonerCachePort;
import com.example.lolserver.domain.summoner.application.port.out.SummonerClientPort;
import com.example.lolserver.domain.summoner.application.port.out.SummonerMessagePort;
import com.example.lolserver.domain.summoner.application.port.out.SummonerPersistencePort;
import com.example.lolserver.domain.summoner.domain.LeagueSummoner;
import com.example.lolserver.domain.summoner.domain.Summoner;
import com.example.lolserver.domain.summoner.domain.SummonerRenewal;
import com.example.lolserver.domain.summoner.domain.vo.GameName;
import com.example.lolserver.support.error.CoreException;
import com.example.lolserver.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class SummonerServiceTest {

    @Mock
    private SummonerPersistencePort summonerPersistencePort;

    @Mock
    private SummonerClientPort summonerClientPort;

    @Mock
    private SummonerCachePort summonerCachePort;

    @Mock
    private SummonerMessagePort summonerMessagePort;

    @InjectMocks
    private SummonerService summonerService;

    // ========== getSummoner 테스트 ==========

    @DisplayName("DB에 소환사가 존재하면 DB에서 조회한 결과를 반환한다")
    @Test
    void getSummoner_DB존재_소환사응답반환() {
        // given
        GameName gameName = new GameName("TestPlayer", "KR1");
        String region = "kr";

        Summoner summoner = createSummoner("puuid-123", "TestPlayer", "KR1");
        given(summonerPersistencePort.getSummoner("TestPlayer", "KR1", region))
                .willReturn(Optional.of(summoner));

        // when
        SummonerResponse result = summonerService.getSummoner(gameName, region);

        // then
        assertThat(result.getGameName()).isEqualTo("TestPlayer");
        assertThat(result.getTagLine()).isEqualTo("KR1");
        then(summonerPersistencePort).should().getSummoner("TestPlayer", "KR1", region);
        then(summonerClientPort).should(never()).getSummoner(any(), any(), any());
    }

    @DisplayName("DB에 소환사가 없으면 클라이언트에서 조회한 결과를 반환한다")
    @Test
    void getSummoner_DB없음_클라이언트조회후반환() {
        // given
        GameName gameName = new GameName("NewPlayer", "KR1");
        String region = "kr";
        String lockKey = "NewPlayer:KR1:kr";

        Summoner summoner = createSummoner("puuid-456", "NewPlayer", "KR1");
        given(summonerPersistencePort.getSummoner("NewPlayer", "KR1", region))
                .willReturn(Optional.empty());
        given(summonerCachePort.tryLock(lockKey)).willReturn(true);
        given(summonerClientPort.getSummoner("NewPlayer", "KR1", region))
                .willReturn(Optional.of(summoner));

        // when
        SummonerResponse result = summonerService.getSummoner(gameName, region);

        // then
        assertThat(result.getGameName()).isEqualTo("NewPlayer");
        then(summonerCachePort).should().tryLock(lockKey);
        then(summonerClientPort).should().getSummoner("NewPlayer", "KR1", region);
        then(summonerCachePort).should().unlock(lockKey);
    }

    @DisplayName("DB와 클라이언트 모두에 소환사가 없으면 예외가 발생한다")
    @Test
    void getSummoner_둘다없음_예외발생() {
        // given
        GameName gameName = new GameName("UnknownPlayer", "KR1");
        String region = "kr";
        String lockKey = "UnknownPlayer:KR1:kr";

        given(summonerPersistencePort.getSummoner("UnknownPlayer", "KR1", region))
                .willReturn(Optional.empty());
        given(summonerCachePort.tryLock(lockKey)).willReturn(true);
        given(summonerClientPort.getSummoner("UnknownPlayer", "KR1", region))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> summonerService.getSummoner(gameName, region))
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.NOT_FOUND_USER);
    }

    // ========== getAllSummonerAutoComplete 테스트 ==========

    @DisplayName("자동완성 검색 결과가 존재하면 소환사 리스트를 반환한다")
    @Test
    void getAllSummonerAutoComplete_결과존재_자동완성리스트반환() {
        // given
        String query = "Test";
        String region = "kr";

        List<Summoner> summoners = List.of(
                createSummoner("puuid-1", "TestPlayer1", "KR1"),
                createSummoner("puuid-2", "TestPlayer2", "KR2")
        );
        given(summonerPersistencePort.getSummonerAuthComplete(query, region)).willReturn(summoners);

        // when
        List<SummonerAutoResponse> result = summonerService.getAllSummonerAutoComplete(query, region);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getGameName()).isEqualTo("TestPlayer1");
        assertThat(result.get(1).getGameName()).isEqualTo("TestPlayer2");
        then(summonerPersistencePort).should().getSummonerAuthComplete(query, region);
    }

    @DisplayName("자동완성 검색 결과에 리그 정보가 포함되면 티어와 랭크가 추가된다")
    @Test
    void getAllSummonerAutoComplete_리그정보포함_티어랭크추가() {
        // given
        String query = "Ranked";
        String region = "kr";

        LeagueSummoner leagueSummoner = new LeagueSummoner(
                "puuid-ranked", "RANKED_SOLO_5x5", "league-1",
                100, 50, "DIAMOND", "I", 75,
                false, false, false, true
        );

        Summoner summoner = new Summoner(
                "puuid-ranked", 300L, 123, "RankedPlayer", "KR1",
                "kr", "rankedplayer", LocalDateTime.now(), LocalDateTime.now(),
                List.of(leagueSummoner)
        );
        given(summonerPersistencePort.getSummonerAuthComplete(query, region)).willReturn(List.of(summoner));

        // when
        List<SummonerAutoResponse> result = summonerService.getAllSummonerAutoComplete(query, region);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTier()).isEqualTo("DIAMOND");
        assertThat(result.get(0).getRank()).isEqualTo("I");
        assertThat(result.get(0).getLeaguePoints()).isEqualTo(75);
    }

    @DisplayName("자동완성 검색 결과가 없으면 빈 리스트를 반환한다")
    @Test
    void getAllSummonerAutoComplete_결과없음_빈리스트반환() {
        // given
        String query = "NoMatch";
        String region = "kr";

        given(summonerPersistencePort.getSummonerAuthComplete(query, region)).willReturn(Collections.emptyList());

        // when
        List<SummonerAutoResponse> result = summonerService.getAllSummonerAutoComplete(query, region);

        // then
        assertThat(result).isEmpty();
        then(summonerPersistencePort).should().getSummonerAuthComplete(query, region);
    }

    // ========== renewalSummonerInfo 테스트 ==========

    @DisplayName("이미 갱신 중인 경우 PROGRESS 상태를 반환한다")
    @Test
    void renewalSummonerInfo_갱신중_PROGRESS반환() {
        // given
        String platform = "kr";
        String puuid = "puuid-updating";

        given(summonerCachePort.isUpdating(puuid)).willReturn(true);

        // when
        SummonerRenewal result = summonerService.renewalSummonerInfo(platform, puuid);

        // then
        assertThat(result.getStatus()).isEqualTo(RenewalStatus.PROGRESS);
        assertThat(result.getPuuid()).isEqualTo(puuid);
        then(summonerPersistencePort).should(never()).findById(any());
    }

    @DisplayName("갱신이 가능한 경우 갱신 처리 후 PROGRESS 상태를 반환한다")
    @Test
    void renewalSummonerInfo_갱신가능_갱신후PROGRESS반환() {
        // given
        String platform = "kr";
        String puuid = "puuid-renewable";

        LocalDateTime oldRevisionDate = LocalDateTime.now().minusMinutes(10);
        LocalDateTime oldClickDate = LocalDateTime.now().minusSeconds(30);

        Summoner summoner = new Summoner(
                puuid, 100L, 123, "Player", "KR1",
                "kr", "player", oldRevisionDate, oldClickDate, null
        );

        given(summonerCachePort.isUpdating(puuid)).willReturn(false);
        given(summonerCachePort.isClickCooldown(puuid)).willReturn(false);
        given(summonerPersistencePort.findById(puuid)).willReturn(Optional.of(summoner));
        // when
        SummonerRenewal result = summonerService.renewalSummonerInfo(platform, puuid);

        // then
        assertThat(result.getStatus()).isEqualTo(RenewalStatus.PROGRESS);
        then(summonerPersistencePort).should(never()).save(any());
        then(summonerCachePort).should().setClickCooldown(puuid);
        then(summonerCachePort).should().createSummonerRenewal(puuid);
        then(summonerMessagePort).should().sendMessage(any(), any(), any());
    }

    @DisplayName("갱신 조건이 충족되지 않으면 SUCCESS 상태를 반환한다")
    @Test
    void renewalSummonerInfo_갱신불가_SUCCESS반환() {
        // given
        String platform = "kr";
        String puuid = "puuid-recent";

        LocalDateTime recentRevisionDate = LocalDateTime.now().minusSeconds(30);
        LocalDateTime recentClickDate = LocalDateTime.now().minusSeconds(5);

        Summoner summoner = new Summoner(
                puuid, 100L, 123, "Player", "KR1",
                "kr", "player", recentRevisionDate, recentClickDate, null
        );

        given(summonerCachePort.isUpdating(puuid)).willReturn(false);
        given(summonerCachePort.isClickCooldown(puuid)).willReturn(false);
        given(summonerPersistencePort.findById(puuid)).willReturn(Optional.of(summoner));

        // when
        SummonerRenewal result = summonerService.renewalSummonerInfo(platform, puuid);

        // then
        assertThat(result.getStatus()).isEqualTo(RenewalStatus.SUCCESS);
        then(summonerPersistencePort).should(never()).save(any());
        then(summonerCachePort).should(never()).createSummonerRenewal(any());
    }

    @DisplayName("존재하지 않는 puuid로 갱신 시도 시 예외가 발생한다")
    @Test
    void renewalSummonerInfo_puuid없음_예외발생() {
        // given
        String platform = "kr";
        String puuid = "invalid-puuid";

        given(summonerCachePort.isUpdating(puuid)).willReturn(false);
        given(summonerCachePort.isClickCooldown(puuid)).willReturn(false);
        given(summonerPersistencePort.findById(puuid)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> summonerService.renewalSummonerInfo(platform, puuid))
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.NOT_FOUND_PUUID);
    }

    @DisplayName("클릭 쿨다운 중이면 DB 조회 없이 SUCCESS 상태를 반환한다")
    @Test
    void renewalSummonerInfo_클릭쿨다운_SUCCESS반환() {
        // given
        String platform = "kr";
        String puuid = "puuid-cooldown";

        given(summonerCachePort.isUpdating(puuid)).willReturn(false);
        given(summonerCachePort.isClickCooldown(puuid)).willReturn(true);

        // when
        SummonerRenewal result = summonerService.renewalSummonerInfo(platform, puuid);

        // then
        assertThat(result.getStatus()).isEqualTo(RenewalStatus.SUCCESS);
        assertThat(result.getPuuid()).isEqualTo(puuid);
        then(summonerPersistencePort).should(never()).findById(any());
        then(summonerPersistencePort).should(never()).save(any());
    }

    // ========== renewalSummonerStatus 테스트 ==========

    @DisplayName("갱신이 진행 중이면 PROGRESS 상태를 반환한다")
    @Test
    void renewalSummonerStatus_갱신중_PROGRESS반환() {
        // given
        String puuid = "puuid-in-progress";
        given(summonerCachePort.isSummonerRenewal(puuid)).willReturn(true);

        // when
        SummonerRenewal result = summonerService.renewalSummonerStatus(puuid);

        // then
        assertThat(result.getStatus()).isEqualTo(RenewalStatus.PROGRESS);
        assertThat(result.getPuuid()).isEqualTo(puuid);
        then(summonerCachePort).should().isSummonerRenewal(puuid);
    }

    @DisplayName("갱신이 완료되면 SUCCESS 상태를 반환한다")
    @Test
    void renewalSummonerStatus_완료_SUCCESS반환() {
        // given
        String puuid = "puuid-completed";
        given(summonerCachePort.isSummonerRenewal(puuid)).willReturn(false);

        // when
        SummonerRenewal result = summonerService.renewalSummonerStatus(puuid);

        // then
        assertThat(result.getStatus()).isEqualTo(RenewalStatus.SUCCESS);
        assertThat(result.getPuuid()).isEqualTo(puuid);
        then(summonerCachePort).should().isSummonerRenewal(puuid);
    }

    // ========== getSummonerByPuuid 테스트 ==========

    @DisplayName("DB에 puuid로 소환사가 존재하면 소환사 응답을 반환한다")
    @Test
    void getSummonerByPuuid_DB존재_소환사응답반환() {
        // given
        String region = "kr";
        String puuid = "puuid-existing";

        Summoner summoner = createSummoner(puuid, "ExistingPlayer", "KR1");
        given(summonerPersistencePort.findById(puuid)).willReturn(Optional.of(summoner));

        // when
        SummonerResponse result = summonerService.getSummonerByPuuid(region, puuid);

        // then
        assertThat(result.getPuuid()).isEqualTo(puuid);
        assertThat(result.getGameName()).isEqualTo("ExistingPlayer");
        then(summonerPersistencePort).should().findById(puuid);
        then(summonerClientPort).should(never()).getSummonerByPuuid(any(), any());
    }

    @DisplayName("DB에 puuid로 소환사가 없으면 클라이언트에서 조회한다")
    @Test
    void getSummonerByPuuid_DB없음_클라이언트조회후반환() {
        // given
        String region = "kr";
        String puuid = "puuid-new";
        String lockKey = "puuid:" + puuid;

        Summoner summoner = createSummoner(puuid, "NewPlayer", "KR1");
        given(summonerPersistencePort.findById(puuid))
                .willReturn(Optional.empty());
        given(summonerCachePort.tryLock(lockKey)).willReturn(true);
        given(summonerClientPort.getSummonerByPuuid(region, puuid)).willReturn(Optional.of(summoner));

        // when
        SummonerResponse result = summonerService.getSummonerByPuuid(region, puuid);

        // then
        assertThat(result.getPuuid()).isEqualTo(puuid);
        then(summonerCachePort).should().tryLock(lockKey);
        then(summonerClientPort).should().getSummonerByPuuid(region, puuid);
        then(summonerCachePort).should().unlock(lockKey);
    }

    @DisplayName("DB와 클라이언트 모두에 puuid가 없으면 예외가 발생한다")
    @Test
    void getSummonerByPuuid_둘다없음_예외발생() {
        // given
        String region = "kr";
        String puuid = "invalid-puuid";
        String lockKey = "puuid:" + puuid;

        given(summonerPersistencePort.findById(puuid)).willReturn(Optional.empty());
        given(summonerCachePort.tryLock(lockKey)).willReturn(true);
        given(summonerClientPort.getSummonerByPuuid(region, puuid)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> summonerService.getSummonerByPuuid(region, puuid))
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.NOT_FOUND_PUUID);
    }

    // ========== getRefreshingSummoners 테스트 ==========

    @DisplayName("갱신 중인 소환사가 없으면 빈 리스트를 반환한다")
    @Test
    void getRefreshingSummoners_없음_빈리스트반환() {
        // given
        given(summonerCachePort.getRefreshingPuuids()).willReturn(Collections.emptySet());

        // when
        List<SummonerRenewalInfoResponse> result = summonerService.getRefreshingSummoners();

        // then
        assertThat(result).isEmpty();
        then(summonerPersistencePort).should(never()).findAllByPuuidIn(any());
    }

    @DisplayName("갱신 중인 소환사가 DB에 존재하면 소환사 정보를 포함하여 반환한다")
    @Test
    void getRefreshingSummoners_DB존재_소환사정보포함반환() {
        // given
        Set<String> puuids = Set.of("puuid-1", "puuid-2");
        Summoner summoner1 = createSummoner("puuid-1", "Player1", "KR1");
        Summoner summoner2 = createSummoner("puuid-2", "Player2", "KR2");

        given(summonerCachePort.getRefreshingPuuids()).willReturn(puuids);
        given(summonerPersistencePort.findAllByPuuidIn(puuids)).willReturn(List.of(summoner1, summoner2));

        // when
        List<SummonerRenewalInfoResponse> result = summonerService.getRefreshingSummoners();

        // then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(SummonerRenewalInfoResponse::getPuuid)
                .containsExactlyInAnyOrder("puuid-1", "puuid-2");
        assertThat(result).allSatisfy(r -> assertThat(r.getGameName()).isNotNull());
    }

    @DisplayName("갱신 중인 소환사가 DB에 없으면 puuid만 포함하여 반환한다")
    @Test
    void getRefreshingSummoners_DB없음_puuid만반환() {
        // given
        Set<String> puuids = Set.of("unknown-puuid");

        given(summonerCachePort.getRefreshingPuuids()).willReturn(puuids);
        given(summonerPersistencePort.findAllByPuuidIn(puuids)).willReturn(Collections.emptyList());

        // when
        List<SummonerRenewalInfoResponse> result = summonerService.getRefreshingSummoners();

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPuuid()).isEqualTo("unknown-puuid");
        assertThat(result.get(0).getGameName()).isNull();
        assertThat(result.get(0).getTagLine()).isNull();
    }

    // ========== Helper Methods ==========

    private Summoner createSummoner(String puuid, String gameName, String tagLine) {
        return new Summoner(
                puuid, 100L, 123, gameName, tagLine,
                "kr", gameName.toLowerCase(), LocalDateTime.now(), LocalDateTime.now(), null
        );
    }
}
