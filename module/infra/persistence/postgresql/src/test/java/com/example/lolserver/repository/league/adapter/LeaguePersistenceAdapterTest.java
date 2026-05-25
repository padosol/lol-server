package com.example.lolserver.repository.league.adapter;

import com.example.lolserver.domain.league.domain.League;
import com.example.lolserver.domain.league.domain.vo.LeagueHistory;
import com.example.lolserver.repository.config.RepositoryTestBase;
import com.example.lolserver.repository.league.LeagueSummonerHistoryRepository;
import com.example.lolserver.repository.league.LeagueSummonerRepository;
import com.example.lolserver.repository.league.entity.LeagueSummonerEntity;
import com.example.lolserver.repository.league.entity.LeagueSummonerHistoryEntity;
import com.example.lolserver.repository.league.mapper.LeagueDomainMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LeaguePersistenceAdapterTest extends RepositoryTestBase {

    @Autowired
    private LeagueSummonerRepository leagueSummonerRepository;

    @Autowired
    private LeagueSummonerHistoryRepository leagueSummonerHistoryRepository;

    @Autowired
    private LeagueDomainMapper leagueDomainMapper;

    @Autowired
    private EntityManager entityManager;

    private LeaguePersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new LeaguePersistenceAdapter(
                leagueSummonerRepository,
                leagueSummonerHistoryRepository,
                leagueDomainMapper
        );
    }

    @DisplayName("PUUID로 리그 정보를 조회하면 도메인 객체 리스트를 반환한다")
    @Test
    void findAllLeaguesByPuuid_validPuuid_returnsDomainList() {
        // given
        String puuid = "test-puuid-123";
        LeagueSummonerEntity soloLeague = LeagueSummonerEntity.builder()
                .puuid(puuid)
                .queue("RANKED_SOLO_5x5")
                .leagueId("league-solo")
                .wins(100)
                .losses(50)
                .tier("DIAMOND")
                .rank("I")
                .leaguePoints(75)
                .absolutePoints(7175)
                .veteran(false)
                .inactive(false)
                .freshBlood(true)
                .hotStreak(false)
                .build();

        LeagueSummonerEntity flexLeague = LeagueSummonerEntity.builder()
                .puuid(puuid)
                .queue("RANKED_FLEX_SR")
                .leagueId("league-flex")
                .wins(60)
                .losses(40)
                .tier("PLATINUM")
                .rank("II")
                .leaguePoints(50)
                .absolutePoints(5250)
                .veteran(true)
                .inactive(false)
                .freshBlood(false)
                .hotStreak(true)
                .build();

        leagueSummonerRepository.saveAll(List.of(soloLeague, flexLeague));

        // when
        List<League> result = adapter.findAllLeaguesByPuuid(puuid);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(League::getPuuid)
                .containsOnly(puuid);
        assertThat(result).extracting(League::getTier)
                .containsExactlyInAnyOrder("DIAMOND", "PLATINUM");
    }

    @DisplayName("ID 목록으로 히스토리를 조회하면 생성일 내림차순으로 정렬된 도메인 객체를 반환한다")
    @Test
    void findAllHistoryByLeagueSummonerIds_validIds_returnsOrderedHistory() {
        // given
        LeagueSummonerEntity leagueSummoner = LeagueSummonerEntity.builder()
                .puuid("history-test-puuid")
                .queue("RANKED_SOLO_5x5")
                .leagueId("league-history")
                .wins(50)
                .losses(30)
                .tier("GOLD")
                .rank("I")
                .leaguePoints(25)
                .absolutePoints(4125)
                .veteran(false)
                .inactive(false)
                .freshBlood(false)
                .hotStreak(false)
                .build();
        LeagueSummonerEntity savedSummoner = leagueSummonerRepository.save(leagueSummoner);
        entityManager.flush();

        LeagueSummonerHistoryEntity history1 = createHistoryEntity(savedSummoner.getId(), "GOLD", "II", 40, 30);
        LeagueSummonerHistoryEntity history2 = createHistoryEntity(savedSummoner.getId(), "GOLD", "I", 50, 30);

        leagueSummonerHistoryRepository.saveAll(List.of(history1, history2));
        entityManager.flush();
        entityManager.clear();

        // when
        List<LeagueHistory> result = adapter.findAllHistoryByLeagueSummonerIds(List.of(savedSummoner.getId()));

        // then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(LeagueHistory::tier)
                .containsExactlyInAnyOrder("GOLD", "GOLD");
    }

    @DisplayName("리그별 최신 history 조회는 25건 중 createdAt 내림차순 최신 20건만 반환하고 오래된 5건은 제외한다")
    @Test
    void findRecentHistoryByLeagueSummonerId_returnsLatest20ByCreatedAtDesc() {
        // given
        LeagueSummonerEntity savedSummoner = leagueSummonerRepository.save(
                buildLeagueSummoner("recent-history-puuid", "RANKED_SOLO_5x5"));
        entityManager.flush();

        // leaguePoints = i, createdAt = base + i분  → i 가 클수록 최신 (총 25건, i: 0~24)
        LocalDateTime base = LocalDateTime.of(2026, 1, 1, 0, 0, 0);
        List<LeagueSummonerHistoryEntity> histories = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            histories.add(createHistoryEntityAt(savedSummoner.getId(), i, base.plusMinutes(i)));
        }
        leagueSummonerHistoryRepository.saveAll(histories);
        entityManager.flush();
        entityManager.clear();

        // when
        List<LeagueHistory> result = adapter.findRecentHistoryByLeagueSummonerId(savedSummoner.getId());

        // then
        assertThat(result).hasSize(20);
        // 최신순(createdAt DESC): leaguePoints 24, 23, ... , 5
        assertThat(result).extracting(LeagueHistory::leaguePoints)
                .containsExactly(24, 23, 22, 21, 20, 19, 18, 17, 16, 15,
                        14, 13, 12, 11, 10, 9, 8, 7, 6, 5);
        // 가장 오래된 5건(leaguePoints 0~4)은 제외된다
        assertThat(result).extracting(LeagueHistory::leaguePoints)
                .doesNotContain(0, 1, 2, 3, 4);
        // 정렬 키는 createdAt 이며 내림차순이다
        assertThat(result).extracting(LeagueHistory::createdAt)
                .isSortedAccordingTo(Comparator.reverseOrder());
    }

    @DisplayName("리그별 최신 history 조회는 대상 leagueSummonerId 의 history 만 반환하고 다른 리그 데이터는 섞이지 않는다")
    @Test
    void findRecentHistoryByLeagueSummonerId_filtersByLeagueSummonerId() {
        // given
        LeagueSummonerEntity target = leagueSummonerRepository.save(
                buildLeagueSummoner("filter-target-puuid", "RANKED_SOLO_5x5"));
        LeagueSummonerEntity other = leagueSummonerRepository.save(
                buildLeagueSummoner("filter-other-puuid", "RANKED_FLEX_SR"));
        entityManager.flush();

        LocalDateTime base = LocalDateTime.of(2026, 1, 1, 0, 0, 0);
        leagueSummonerHistoryRepository.saveAll(List.of(
                createHistoryEntityAt(target.getId(), 10, base),
                createHistoryEntityAt(target.getId(), 11, base.plusMinutes(1)),
                createHistoryEntityAt(other.getId(), 99, base.plusMinutes(2))
        ));
        entityManager.flush();
        entityManager.clear();

        // when
        List<LeagueHistory> result = adapter.findRecentHistoryByLeagueSummonerId(target.getId());

        // then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(LeagueHistory::leagueSummonerId)
                .containsOnly(target.getId());
        // 다른 리그(other)의 history(leaguePoints 99)는 누수되지 않는다
        assertThat(result).extracting(LeagueHistory::leaguePoints)
                .doesNotContain(99);
    }

    private LeagueSummonerEntity buildLeagueSummoner(String puuid, String queue) {
        return LeagueSummonerEntity.builder()
                .puuid(puuid)
                .queue(queue)
                .leagueId("league-recent")
                .wins(50)
                .losses(30)
                .tier("GOLD")
                .rank("I")
                .leaguePoints(25)
                .absolutePoints(4125)
                .veteran(false)
                .inactive(false)
                .freshBlood(false)
                .hotStreak(false)
                .build();
    }

    private LeagueSummonerHistoryEntity createHistoryEntityAt(Long leagueSummonerId, int leaguePoints, LocalDateTime createdAt) {
        LeagueSummonerHistoryEntity entity = new LeagueSummonerHistoryEntity();
        ReflectionTestUtils.setField(entity, "leagueSummonerId", leagueSummonerId);
        ReflectionTestUtils.setField(entity, "puuid", "history-test-puuid");
        ReflectionTestUtils.setField(entity, "queue", "RANKED_SOLO_5x5");
        ReflectionTestUtils.setField(entity, "leagueId", "league-history");
        ReflectionTestUtils.setField(entity, "tier", "GOLD");
        ReflectionTestUtils.setField(entity, "rank", "I");
        ReflectionTestUtils.setField(entity, "wins", 40);
        ReflectionTestUtils.setField(entity, "losses", 30);
        ReflectionTestUtils.setField(entity, "leaguePoints", leaguePoints);
        ReflectionTestUtils.setField(entity, "absolutePoints", 4125L);
        ReflectionTestUtils.setField(entity, "veteran", false);
        ReflectionTestUtils.setField(entity, "inactive", false);
        ReflectionTestUtils.setField(entity, "freshBlood", false);
        ReflectionTestUtils.setField(entity, "hotStreak", false);
        ReflectionTestUtils.setField(entity, "createdAt", createdAt);
        return entity;
    }

    private LeagueSummonerHistoryEntity createHistoryEntity(Long leagueSummonerId, String tier, String rank, int wins, int losses) {
        LeagueSummonerHistoryEntity entity = new LeagueSummonerHistoryEntity();
        ReflectionTestUtils.setField(entity, "leagueSummonerId", leagueSummonerId);
        ReflectionTestUtils.setField(entity, "puuid", "history-test-puuid");
        ReflectionTestUtils.setField(entity, "queue", "RANKED_SOLO_5x5");
        ReflectionTestUtils.setField(entity, "leagueId", "league-history");
        ReflectionTestUtils.setField(entity, "tier", tier);
        ReflectionTestUtils.setField(entity, "rank", rank);
        ReflectionTestUtils.setField(entity, "wins", wins);
        ReflectionTestUtils.setField(entity, "losses", losses);
        ReflectionTestUtils.setField(entity, "leaguePoints", 25);
        ReflectionTestUtils.setField(entity, "absolutePoints", 4125L);
        ReflectionTestUtils.setField(entity, "veteran", false);
        ReflectionTestUtils.setField(entity, "inactive", false);
        ReflectionTestUtils.setField(entity, "freshBlood", false);
        ReflectionTestUtils.setField(entity, "hotStreak", false);
        return entity;
    }
}
