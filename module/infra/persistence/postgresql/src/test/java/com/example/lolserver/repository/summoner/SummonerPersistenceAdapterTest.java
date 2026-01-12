package com.example.lolserver.repository.summoner;

import com.example.lolserver.domain.summoner.domain.Summoner;
import com.example.lolserver.repository.config.RepositoryTestBase;
import com.example.lolserver.repository.league.LeagueSummonerRepository;
import com.example.lolserver.repository.league.entity.LeagueSummonerEntity;
import com.example.lolserver.repository.summoner.entity.SummonerEntity;
import com.example.lolserver.repository.summoner.repository.SummonerJpaRepository;
import com.example.lolserver.repository.summoner.repository.dsl.SummonerRepositoryCustom;
import com.example.lolserver.repository.summoner.repository.dsl.impl.SummonerRepositoryCustomImpl;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class SummonerPersistenceAdapterTest extends RepositoryTestBase {

    @Autowired
    private SummonerJpaRepository summonerJpaRepository;

    @Autowired
    private LeagueSummonerRepository leagueSummonerRepository;

    @Autowired
    private SummonerMapper summonerMapper;

    @Autowired
    private EntityManager entityManager;

    private SummonerRepositoryCustom summonerRepositoryCustom;
    private SummonerPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        summonerRepositoryCustom = new SummonerRepositoryCustomImpl(queryFactory);
        adapter = new SummonerPersistenceAdapter(
                summonerRepositoryCustom,
                summonerJpaRepository,
                summonerMapper
        );
    }

    @DisplayName("게임명, 태그라인, 지역으로 소환사를 조회하면 도메인 객체를 반환한다")
    @Test
    void getSummoner_validInput_returnsDomainObject() {
        // given
        SummonerEntity summoner = createSummonerEntity("test-puuid-1", "HideOnBush", "KR1", "kr");
        summonerJpaRepository.save(summoner);
        entityManager.flush();
        entityManager.clear();

        // when
        Optional<Summoner> result = adapter.getSummoner("HideOnBush", "KR1", "kr");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getGameName()).isEqualTo("HideOnBush");
        assertThat(result.get().getTagLine()).isEqualTo("KR1");
        assertThat(result.get().getRegion()).isEqualTo("kr");
    }

    @DisplayName("존재하지 않는 소환사를 조회하면 빈 Optional을 반환한다")
    @Test
    void getSummoner_notFound_returnsEmpty() {
        // given
        // no data

        // when
        Optional<Summoner> result = adapter.getSummoner("NonExistent", "KR1", "kr");

        // then
        assertThat(result).isEmpty();
    }

    @DisplayName("자동완성 쿼리로 소환사 목록을 조회한다")
    @Test
    void getSummonerAuthComplete_validQuery_returnsDomainList() {
        // given
        SummonerEntity summoner = createSummonerEntity("autocomplete-puuid", "TestPlayer", "KR1", "kr");
        summoner = summonerJpaRepository.save(summoner);

        LeagueSummonerEntity leagueSummoner = LeagueSummonerEntity.builder()
                .puuid("autocomplete-puuid")
                .queue("RANKED_SOLO_5x5")
                .leagueId("league-autocomplete")
                .wins(50)
                .losses(30)
                .tier("GOLD")
                .rank("I")
                .leaguePoints(75)
                .absolutePoints(4175)
                .veteran(false)
                .inactive(false)
                .freshBlood(false)
                .hotStreak(false)
                .build();
        leagueSummonerRepository.save(leagueSummoner);

        entityManager.flush();
        entityManager.clear();

        // when
        List<Summoner> result = adapter.getSummonerAuthComplete("test", "kr");

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getGameName()).isEqualTo("TestPlayer");
    }

    @DisplayName("PUUID로 소환사를 조회하면 도메인 객체를 반환한다")
    @Test
    void findById_validPuuid_returnsDomainObject() {
        // given
        String puuid = "findbyid-puuid";
        SummonerEntity summoner = createSummonerEntity(puuid, "FindByIdPlayer", "NA1", "na");
        summonerJpaRepository.save(summoner);
        entityManager.flush();
        entityManager.clear();

        // when
        Optional<Summoner> result = adapter.findById(puuid);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getPuuid()).isEqualTo(puuid);
        assertThat(result.get().getGameName()).isEqualTo("FindByIdPlayer");
    }

    @DisplayName("소환사를 저장하면 저장된 도메인 객체를 반환한다")
    @Test
    void save_validSummoner_returnsSavedDomain() {
        // given
        Summoner summoner = new Summoner(
                "save-test-puuid",
                100L,
                1234,
                "SaveTestPlayer",
                "KR1",
                "kr",
                "savetestplayer",
                LocalDateTime.now(),
                LocalDateTime.now(),
                null
        );

        // when
        Summoner result = adapter.save(summoner);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getPuuid()).isEqualTo("save-test-puuid");
        assertThat(result.getGameName()).isEqualTo("SaveTestPlayer");

        // verify persistence
        Optional<SummonerEntity> persisted = summonerJpaRepository.findById("save-test-puuid");
        assertThat(persisted).isPresent();
    }

    private SummonerEntity createSummonerEntity(String puuid, String gameName, String tagLine, String region) {
        return SummonerEntity.builder()
                .puuid(puuid)
                .gameName(gameName)
                .tagLine(tagLine)
                .region(region)
                .summonerLevel(100L)
                .profileIconId(1234)
                .searchName(gameName.toLowerCase().replace(" ", ""))
                .revisionDate(LocalDateTime.now())
                .revisionClickDate(LocalDateTime.now())
                .build();
    }
}
