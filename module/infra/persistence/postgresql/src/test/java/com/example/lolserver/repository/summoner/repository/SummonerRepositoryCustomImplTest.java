package com.example.lolserver.repository.summoner.repository;

import com.example.lolserver.repository.config.RepositoryTestBase;
import com.example.lolserver.repository.league.LeagueSummonerRepository;
import com.example.lolserver.repository.league.entity.LeagueSummonerEntity;
import com.example.lolserver.repository.summoner.entity.SummonerEntity;
import com.example.lolserver.repository.summoner.repository.dsl.impl.SummonerRepositoryCustomImpl;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SummonerRepositoryCustomImplTest extends RepositoryTestBase {

    @Autowired
    private SummonerJpaRepository summonerRepository;

    @Autowired
    private LeagueSummonerRepository leagueSummonerRepository;

    @Autowired
    private JPAQueryFactory jpaQueryFactory;

    private SummonerRepositoryCustomImpl summonerRepositoryCustom;

    @BeforeEach
    void setUp() {
        summonerRepositoryCustom = new SummonerRepositoryCustomImpl(jpaQueryFactory);
    }

    @DisplayName("게임 이름, 태그라인, 리전으로 소환사를 조회한다")
    @Test
    void findAllByGameNameAndTagLineAndRegion_validInput_returnsSummoners() {
        // given
        SummonerEntity summoner = SummonerEntity.builder()
                .puuid("test-puuid-1")
                .gameName("TestPlayer")
                .tagLine("KR1")
                .region("kr")
                .summonerLevel(300L)
                .profileIconId(5001)
                .searchName("testplayer")
                .revisionDate(LocalDateTime.now())
                .lastRiotCallDate(LocalDateTime.now())
                .build();
        summonerRepository.save(summoner);

        // when
        List<SummonerEntity> result = summonerRepositoryCustom
                .findAllByGameNameAndTagLineAndRegion("TestPlayer", "KR1", "kr");

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getGameName()).isEqualTo("TestPlayer");
        assertThat(result.get(0).getTagLine()).isEqualTo("KR1");
    }

    @DisplayName("공백이 포함된 게임 이름도 조회할 수 있다")
    @Test
    void findAllByGameNameAndTagLineAndRegion_gameNameWithSpaces_returnsSummoners() {
        // given
        SummonerEntity summoner = SummonerEntity.builder()
                .puuid("test-puuid-2")
                .gameName("Test Player")
                .tagLine("KR1")
                .region("kr")
                .summonerLevel(300L)
                .profileIconId(5001)
                .searchName("testplayer")
                .revisionDate(LocalDateTime.now())
                .lastRiotCallDate(LocalDateTime.now())
                .build();
        summonerRepository.save(summoner);

        // when
        List<SummonerEntity> result = summonerRepositoryCustom
                .findAllByGameNameAndTagLineAndRegion("TestPlayer", "KR1", "kr");

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getGameName()).isEqualTo("Test Player");
    }

    @DisplayName("대소문자를 구분하지 않고 조회한다")
    @Test
    void findAllByGameNameAndTagLineAndRegion_caseInsensitive_returnsSummoners() {
        // given
        SummonerEntity summoner = SummonerEntity.builder()
                .puuid("test-puuid-3")
                .gameName("TestPlayer")
                .tagLine("KR1")
                .region("kr")
                .summonerLevel(300L)
                .profileIconId(5001)
                .searchName("testplayer")
                .revisionDate(LocalDateTime.now())
                .lastRiotCallDate(LocalDateTime.now())
                .build();
        summonerRepository.save(summoner);

        // when
        List<SummonerEntity> result = summonerRepositoryCustom
                .findAllByGameNameAndTagLineAndRegion("TESTPLAYER", "kr1", "KR");

        // then
        assertThat(result).hasSize(1);
    }

    @DisplayName("존재하지 않는 소환사 조회시 빈 리스트를 반환한다")
    @Test
    void findAllByGameNameAndTagLineAndRegion_notFound_returnsEmptyList() {
        // when
        List<SummonerEntity> result = summonerRepositoryCustom
                .findAllByGameNameAndTagLineAndRegion("NonExistent", "KR1", "kr");

        // then
        assertThat(result).isEmpty();
    }

    @DisplayName("자동완성 검색으로 소환사를 조회한다")
    @Test
    void findAllByGameNameAndTagLineAndRegionLike_validInput_returnsSummoners() {
        // given
        SummonerEntity summoner = SummonerEntity.builder()
                .puuid("test-puuid-4")
                .gameName("TestPlayer")
                .tagLine("KR1")
                .region("kr")
                .summonerLevel(300L)
                .profileIconId(5001)
                .searchName("testplayer")
                .revisionDate(LocalDateTime.now())
                .lastRiotCallDate(LocalDateTime.now())
                .build();
        summonerRepository.save(summoner);

        LeagueSummonerEntity leagueSummoner = LeagueSummonerEntity.builder()
                .puuid("test-puuid-4")
                .queue("RANKED_SOLO_5x5")
                .wins(100)
                .losses(50)
                .tier("DIAMOND")
                .rank("I")
                .leaguePoints(75)
                .build();
        leagueSummonerRepository.save(leagueSummoner);

        // when
        List<SummonerEntity> result = summonerRepositoryCustom
                .findAllByGameNameAndTagLineAndRegionLike("test", "kr");

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getGameName()).isEqualTo("TestPlayer");
    }

    @DisplayName("gameNameLike 메서드가 null 입력시 null을 반환한다")
    @Test
    void gameNameLike_nullInput_returnsNull() {
        // when
        var result = summonerRepositoryCustom.gameNameLike(null);

        // then
        assertThat(result).isNull();
    }

    @DisplayName("gameNameLike 메서드가 빈 문자열 입력시 null을 반환한다")
    @Test
    void gameNameLike_emptyString_returnsNull() {
        // when
        var result = summonerRepositoryCustom.gameNameLike("");

        // then
        assertThat(result).isNull();
    }

    @DisplayName("tagLineLike 메서드가 null 입력시 null을 반환한다")
    @Test
    void tagLineLike_nullInput_returnsNull() {
        // when
        var result = summonerRepositoryCustom.tagLineLike(null);

        // then
        assertThat(result).isNull();
    }

    @DisplayName("gameNameEq 메서드가 null 입력시 null을 반환한다")
    @Test
    void gameNameEq_nullInput_returnsNull() {
        // when
        var result = summonerRepositoryCustom.gameNameEq(null);

        // then
        assertThat(result).isNull();
    }

    @DisplayName("tagLineEq 메서드가 null 입력시 null을 반환한다")
    @Test
    void tagLineEq_nullInput_returnsNull() {
        // when
        var result = summonerRepositoryCustom.tagLineEq(null);

        // then
        assertThat(result).isNull();
    }

    @DisplayName("regionEq 메서드가 null 입력시 null을 반환한다")
    @Test
    void regionEq_nullInput_returnsNull() {
        // when
        var result = summonerRepositoryCustom.regionEq(null);

        // then
        assertThat(result).isNull();
    }
}
