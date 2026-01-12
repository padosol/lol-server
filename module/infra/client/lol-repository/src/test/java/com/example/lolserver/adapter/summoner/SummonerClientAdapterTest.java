package com.example.lolserver.adapter.summoner;

import com.example.lolserver.domain.summoner.domain.Summoner;
import com.example.lolserver.mapper.summoner.SummonerClientMapper;
import com.example.lolserver.restclient.summoner.SummonerRestClient;
import com.example.lolserver.restclient.summoner.model.SummonerVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class SummonerClientAdapterTest {

    @Mock
    private SummonerRestClient summonerRestClient;

    @Mock
    private SummonerClientMapper summonerClientMapper;

    private SummonerClientAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new SummonerClientAdapter(summonerRestClient, summonerClientMapper);
    }

    @DisplayName("게임 이름과 태그로 소환사를 조회하면 도메인 객체를 반환한다")
    @Test
    void getSummoner_validParams_returnsSummoner() {
        // given
        String gameName = "Hide on bush";
        String tagLine = "KR1";
        String region = "kr";

        SummonerVO summonerVO = createSummonerVO("test-puuid", gameName, tagLine);
        Summoner expectedSummoner = createSummoner("test-puuid", gameName, tagLine);

        given(summonerRestClient.getSummonerByGameNameAndTagLine(region, gameName, tagLine))
                .willReturn(summonerVO);
        given(summonerClientMapper.toDomain(summonerVO)).willReturn(expectedSummoner);

        // when
        Optional<Summoner> result = adapter.getSummoner(gameName, tagLine, region);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getPuuid()).isEqualTo("test-puuid");
        assertThat(result.get().getGameName()).isEqualTo(gameName);
        assertThat(result.get().getTagLine()).isEqualTo(tagLine);
        then(summonerRestClient).should().getSummonerByGameNameAndTagLine(region, gameName, tagLine);
        then(summonerClientMapper).should().toDomain(summonerVO);
    }

    @DisplayName("존재하지 않는 소환사를 조회하면 빈 Optional을 반환한다")
    @Test
    void getSummoner_nonExistingSummoner_returnsEmpty() {
        // given
        String gameName = "NonExistentPlayer";
        String tagLine = "0000";
        String region = "kr";

        given(summonerRestClient.getSummonerByGameNameAndTagLine(region, gameName, tagLine))
                .willReturn(null);

        // when
        Optional<Summoner> result = adapter.getSummoner(gameName, tagLine, region);

        // then
        assertThat(result).isEmpty();
        then(summonerRestClient).should().getSummonerByGameNameAndTagLine(region, gameName, tagLine);
    }

    @DisplayName("PUUID로 소환사를 조회하면 도메인 객체를 반환한다")
    @Test
    void getSummonerByPuuid_validPuuid_returnsSummoner() {
        // given
        String region = "kr";
        String puuid = "test-puuid-123";

        SummonerVO summonerVO = createSummonerVO(puuid, "TestPlayer", "KR1");
        Summoner expectedSummoner = createSummoner(puuid, "TestPlayer", "KR1");

        given(summonerRestClient.getSummonerByPuuid(region, puuid)).willReturn(summonerVO);
        given(summonerClientMapper.toDomain(summonerVO)).willReturn(expectedSummoner);

        // when
        Optional<Summoner> result = adapter.getSummonerByPuuid(region, puuid);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getPuuid()).isEqualTo(puuid);
        then(summonerRestClient).should().getSummonerByPuuid(region, puuid);
        then(summonerClientMapper).should().toDomain(summonerVO);
    }

    @DisplayName("존재하지 않는 PUUID로 조회하면 빈 Optional을 반환한다")
    @Test
    void getSummonerByPuuid_nonExistingPuuid_returnsEmpty() {
        // given
        String region = "kr";
        String puuid = "non-existent-puuid";

        given(summonerRestClient.getSummonerByPuuid(region, puuid)).willReturn(null);

        // when
        Optional<Summoner> result = adapter.getSummonerByPuuid(region, puuid);

        // then
        assertThat(result).isEmpty();
        then(summonerRestClient).should().getSummonerByPuuid(region, puuid);
    }

    @DisplayName("다른 지역의 소환사를 조회할 수 있다")
    @Test
    void getSummoner_differentRegion_returnsSummoner() {
        // given
        String gameName = "Doublelift";
        String tagLine = "NA1";
        String region = "na1";

        SummonerVO summonerVO = createSummonerVO("na-puuid", gameName, tagLine);
        Summoner expectedSummoner = createSummoner("na-puuid", gameName, tagLine);

        given(summonerRestClient.getSummonerByGameNameAndTagLine(region, gameName, tagLine))
                .willReturn(summonerVO);
        given(summonerClientMapper.toDomain(summonerVO)).willReturn(expectedSummoner);

        // when
        Optional<Summoner> result = adapter.getSummoner(gameName, tagLine, region);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getGameName()).isEqualTo(gameName);
        then(summonerRestClient).should().getSummonerByGameNameAndTagLine(region, gameName, tagLine);
    }

    private SummonerVO createSummonerVO(String puuid, String gameName, String tagLine) {
        SummonerVO vo = new SummonerVO();
        ReflectionTestUtils.setField(vo, "puuid", puuid);
        ReflectionTestUtils.setField(vo, "gameName", gameName);
        ReflectionTestUtils.setField(vo, "tagLine", tagLine);
        ReflectionTestUtils.setField(vo, "profileIconId", 1234);
        ReflectionTestUtils.setField(vo, "summonerLevel", 100L);
        ReflectionTestUtils.setField(vo, "revisionDate", LocalDateTime.now());
        return vo;
    }

    private Summoner createSummoner(String puuid, String gameName, String tagLine) {
        Summoner summoner = new Summoner();
        summoner.setPuuid(puuid);
        summoner.setGameName(gameName);
        summoner.setTagLine(tagLine);
        summoner.setProfileIconId(1234);
        summoner.setSummonerLevel(100L);
        summoner.setRevisionDate(LocalDateTime.now());
        return summoner;
    }
}
