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
