package com.example.lolserver.mapper.summoner;

import com.example.lolserver.domain.summoner.domain.Summoner;
import com.example.lolserver.restclient.summoner.model.SummonerVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class SummonerClientMapperTest {

    private final SummonerClientMapper mapper = SummonerClientMapper.INSTANCE;

    @DisplayName("SummonerVO를 Summoner 도메인으로 변환한다")
    @Test
    void toDomain_validVO_returnsSummoner() {
        // given
        SummonerVO vo = new SummonerVO();
        ReflectionTestUtils.setField(vo, "puuid", "test-puuid-123");
        ReflectionTestUtils.setField(vo, "gameName", "Hide on bush");
        ReflectionTestUtils.setField(vo, "tagLine", "KR1");
        ReflectionTestUtils.setField(vo, "profileIconId", 1234);
        ReflectionTestUtils.setField(vo, "summonerLevel", 500L);
        ReflectionTestUtils.setField(vo, "revisionDate", LocalDateTime.of(2024, 1, 15, 10, 30));

        // when
        Summoner result = mapper.toDomain(vo);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getPuuid()).isEqualTo("test-puuid-123");
        assertThat(result.getGameName()).isEqualTo("Hide on bush");
        assertThat(result.getTagLine()).isEqualTo("KR1");
        assertThat(result.getProfileIconId()).isEqualTo(1234);
        assertThat(result.getSummonerLevel()).isEqualTo(500L);
        assertThat(result.getRevisionDate()).isEqualTo(LocalDateTime.of(2024, 1, 15, 10, 30));
    }

    @DisplayName("SummonerVO 변환 시 무시되는 필드들은 null이다")
    @Test
    void toDomain_ignoredFields_areNull() {
        // given
        SummonerVO vo = new SummonerVO();
        ReflectionTestUtils.setField(vo, "puuid", "test-puuid");
        ReflectionTestUtils.setField(vo, "gameName", "TestPlayer");
        ReflectionTestUtils.setField(vo, "tagLine", "NA1");
        ReflectionTestUtils.setField(vo, "profileIconId", 100);
        ReflectionTestUtils.setField(vo, "summonerLevel", 100L);

        // when
        Summoner result = mapper.toDomain(vo);

        // then
        assertThat(result.getLeagueSummoners()).isNull();
        assertThat(result.getLastRiotCallDate()).isNull();
        assertThat(result.getSearchName()).isNull();
        assertThat(result.getPlatformId()).isNull();
    }

    @DisplayName("null SummonerVO는 null을 반환한다")
    @Test
    void toDomain_nullVO_returnsNull() {
        // when
        Summoner result = mapper.toDomain(null);

        // then
        assertThat(result).isNull();
    }

    @DisplayName("INSTANCE를 통해 매퍼를 가져올 수 있다")
    @Test
    void instance_isNotNull() {
        // then
        assertThat(SummonerClientMapper.INSTANCE).isNotNull();
    }
}
