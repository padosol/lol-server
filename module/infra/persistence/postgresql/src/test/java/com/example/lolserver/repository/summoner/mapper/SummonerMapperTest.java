package com.example.lolserver.repository.summoner.mapper;

import com.example.lolserver.domain.summoner.domain.LeagueSummoner;
import com.example.lolserver.domain.summoner.domain.Summoner;
import com.example.lolserver.repository.league.entity.LeagueSummonerEntity;
import com.example.lolserver.repository.summoner.SummonerMapper;
import com.example.lolserver.repository.summoner.entity.SummonerEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SummonerMapperTest {

    private final SummonerMapper mapper = SummonerMapper.INSTANCE;

    @DisplayName("SummonerEntity를 Summoner 도메인으로 변환한다")
    @Test
    void toDomain_validEntity_returnsSummoner() {
        // given
        LocalDateTime now = LocalDateTime.now();
        SummonerEntity entity = SummonerEntity.builder()
                .puuid("test-puuid")
                .summonerLevel(300L)
                .profileIconId(5001)
                .gameName("TestPlayer")
                .tagLine("KR1")
                .platformId("kr")
                .searchName("testplayer")
                .revisionDate(now)
                .lastRiotCallDate(now.minusMinutes(5))
                .build();

        // when
        Summoner result = mapper.toDomain(entity);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getPuuid()).isEqualTo("test-puuid");
        assertThat(result.getSummonerLevel()).isEqualTo(300L);
        assertThat(result.getProfileIconId()).isEqualTo(5001);
        assertThat(result.getGameName()).isEqualTo("TestPlayer");
        assertThat(result.getTagLine()).isEqualTo("KR1");
        assertThat(result.getPlatformId()).isEqualTo("kr");
        assertThat(result.getSearchName()).isEqualTo("testplayer");
        assertThat(result.getRevisionDate()).isEqualTo(now);
        assertThat(result.getLastRiotCallDate()).isEqualTo(now.minusMinutes(5));
    }

    @DisplayName("LeagueSummonerEntity를 LeagueSummoner 도메인으로 변환한다")
    @Test
    void toDomain_leagueSummonerEntity_returnsLeagueSummoner() {
        // given
        LeagueSummonerEntity entity = LeagueSummonerEntity.builder()
                .puuid("test-puuid")
                .queue("RANKED_SOLO_5x5")
                .leagueId("league-123")
                .wins(100)
                .losses(50)
                .tier("DIAMOND")
                .rank("I")
                .leaguePoints(75)
                .veteran(true)
                .inactive(false)
                .freshBlood(false)
                .hotStreak(true)
                .build();

        // when
        LeagueSummoner result = mapper.toDomain(entity);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getPuuid()).isEqualTo("test-puuid");
        assertThat(result.getQueue()).isEqualTo("RANKED_SOLO_5x5");
        assertThat(result.getLeagueId()).isEqualTo("league-123");
        assertThat(result.getWins()).isEqualTo(100);
        assertThat(result.getLosses()).isEqualTo(50);
        assertThat(result.getTier()).isEqualTo("DIAMOND");
        assertThat(result.getRank()).isEqualTo("I");
        assertThat(result.getLeaguePoints()).isEqualTo(75);
        assertThat(result.isVeteran()).isTrue();
        assertThat(result.isInactive()).isFalse();
        assertThat(result.isFreshBlood()).isFalse();
        assertThat(result.isHotStreak()).isTrue();
    }

    @DisplayName("Summoner 도메인을 SummonerEntity로 변환한다")
    @Test
    void toEntity_validSummoner_returnsSummonerEntity() {
        // given
        LocalDateTime now = LocalDateTime.now();
        Summoner summoner = new Summoner();
        summoner.setPuuid("test-puuid");
        summoner.setSummonerLevel(300L);
        summoner.setProfileIconId(5001);
        summoner.setGameName("TestPlayer");
        summoner.setTagLine("KR1");
        summoner.setPlatformId("kr");
        summoner.setSearchName("testplayer");
        summoner.setRevisionDate(now);
        summoner.setLastRiotCallDate(now.minusMinutes(5));

        // when
        SummonerEntity result = mapper.toEntity(summoner);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getPuuid()).isEqualTo("test-puuid");
        assertThat(result.getSummonerLevel()).isEqualTo(300L);
        assertThat(result.getProfileIconId()).isEqualTo(5001);
        assertThat(result.getGameName()).isEqualTo("TestPlayer");
        assertThat(result.getTagLine()).isEqualTo("KR1");
        assertThat(result.getPlatformId()).isEqualTo("kr");
    }

    @DisplayName("SummonerEntity 리스트를 Summoner 도메인 리스트로 변환한다")
    @Test
    void toDomainList_entityList_returnsSummonerList() {
        // given
        LocalDateTime now = LocalDateTime.now();
        SummonerEntity entity1 = SummonerEntity.builder()
                .puuid("puuid-1")
                .summonerLevel(200L)
                .gameName("Player1")
                .tagLine("KR1")
                .platformId("kr")
                .revisionDate(now)
                .build();

        SummonerEntity entity2 = SummonerEntity.builder()
                .puuid("puuid-2")
                .summonerLevel(300L)
                .gameName("Player2")
                .tagLine("KR2")
                .platformId("kr")
                .revisionDate(now)
                .build();

        List<SummonerEntity> entities = List.of(entity1, entity2);

        // when
        List<Summoner> result = mapper.toDomainList(entities);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getPuuid()).isEqualTo("puuid-1");
        assertThat(result.get(0).getGameName()).isEqualTo("Player1");
        assertThat(result.get(1).getPuuid()).isEqualTo("puuid-2");
        assertThat(result.get(1).getGameName()).isEqualTo("Player2");
    }

    @DisplayName("빈 SummonerEntity 리스트 변환시 빈 리스트를 반환한다")
    @Test
    void toDomainList_emptyList_returnsEmptyList() {
        // given
        List<SummonerEntity> entities = List.of();

        // when
        List<Summoner> result = mapper.toDomainList(entities);

        // then
        assertThat(result).isEmpty();
    }

    @DisplayName("SummonerEntity에 LeagueSummonerEntity가 있는 경우 함께 변환한다")
    @Test
    void toDomain_entityWithLeagueSummoners_returnsSummonerWithLeagueSummoners() {
        // given
        LeagueSummonerEntity leagueEntity = LeagueSummonerEntity.builder()
                .puuid("test-puuid")
                .queue("RANKED_SOLO_5x5")
                .wins(100)
                .losses(50)
                .tier("DIAMOND")
                .rank("I")
                .leaguePoints(75)
                .build();

        LocalDateTime now = LocalDateTime.now();
        SummonerEntity entity = SummonerEntity.builder()
                .puuid("test-puuid")
                .summonerLevel(300L)
                .gameName("TestPlayer")
                .tagLine("KR1")
                .platformId("kr")
                .revisionDate(now)
                .leagueSummonerEntities(List.of(leagueEntity))
                .build();

        // when
        Summoner result = mapper.toDomain(entity);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getPuuid()).isEqualTo("test-puuid");
        assertThat(result.getLeagueSummoners()).hasSize(1);
        assertThat(result.getLeagueSummoners().get(0).getQueue()).isEqualTo("RANKED_SOLO_5x5");
        assertThat(result.getLeagueSummoners().get(0).getTier()).isEqualTo("DIAMOND");
    }
}
