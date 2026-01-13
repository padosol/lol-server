package com.example.lolserver.repository.league.mapper;

import com.example.lolserver.domain.league.domain.League;
import com.example.lolserver.domain.league.domain.vo.LeagueHistory;
import com.example.lolserver.repository.league.entity.LeagueSummonerEntity;
import com.example.lolserver.repository.league.entity.LeagueSummonerHistoryEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LeagueDomainMapperTest {

    private final LeagueDomainMapper mapper = LeagueDomainMapper.INSTANCE;

    @DisplayName("LeagueSummonerEntity를 League 도메인으로 변환한다")
    @Test
    void toDomain_validEntity_returnsLeague() {
        // given
        LeagueSummonerEntity entity = LeagueSummonerEntity.builder()
                .id(1L)
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
        League result = mapper.toDomain(entity);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
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

    @DisplayName("승률 계산이 정확하게 이루어진다")
    @Test
    void toDomain_winsAndLosses_calculatesWinRateCorrectly() {
        // given
        LeagueSummonerEntity entity = LeagueSummonerEntity.builder()
                .wins(100)
                .losses(50)
                .build();

        // when
        League result = mapper.toDomain(entity);

        // then
        // 100 / (100 + 50) = 0.67 (rounded to 2 decimal places)
        assertThat(result.getWinRate()).isEqualByComparingTo(new BigDecimal("0.67"));
    }

    @DisplayName("승률 계산시 총 게임이 0인 경우 0을 반환한다")
    @Test
    void toDomain_noGames_returnsZeroWinRate() {
        // given
        LeagueSummonerEntity entity = LeagueSummonerEntity.builder()
                .wins(0)
                .losses(0)
                .build();

        // when
        League result = mapper.toDomain(entity);

        // then
        assertThat(result.getWinRate()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @DisplayName("승률이 정확히 50%인 경우")
    @Test
    void toDomain_equalWinsAndLosses_returns50PercentWinRate() {
        // given
        LeagueSummonerEntity entity = LeagueSummonerEntity.builder()
                .wins(50)
                .losses(50)
                .build();

        // when
        League result = mapper.toDomain(entity);

        // then
        assertThat(result.getWinRate()).isEqualByComparingTo(new BigDecimal("0.50"));
    }

    @DisplayName("LeagueSummonerHistoryEntity를 LeagueHistory로 변환한다")
    @Test
    void toDomain_historyEntity_returnsLeagueHistory() throws Exception {
        // given
        LocalDateTime now = LocalDateTime.now();
        LeagueSummonerHistoryEntity entity = createHistoryEntity(
                1L, "test-puuid", "RANKED_SOLO_5x5", "league-123",
                100, 50, "DIAMOND", "I", 75, 2475L, now
        );

        // when
        LeagueHistory result = mapper.toDomain(entity);

        // then
        assertThat(result).isNotNull();
        assertThat(result.leagueSummonerId()).isEqualTo(1L);
        assertThat(result.puuid()).isEqualTo("test-puuid");
        assertThat(result.queue()).isEqualTo("RANKED_SOLO_5x5");
        assertThat(result.wins()).isEqualTo(100);
        assertThat(result.losses()).isEqualTo(50);
        assertThat(result.tier()).isEqualTo("DIAMOND");
        assertThat(result.rank()).isEqualTo("I");
        assertThat(result.leaguePoints()).isEqualTo(75);
        assertThat(result.absolutePoints()).isEqualTo(2475);
    }

    @DisplayName("LeagueSummonerHistoryEntity 리스트를 LeagueHistory 리스트로 변환한다")
    @Test
    void toDomainHistoryList_entityList_returnsDomainList() throws Exception {
        // given
        LeagueSummonerHistoryEntity entity1 = createHistoryEntity(
                1L, "puuid-1", "RANKED_SOLO_5x5", "league-1",
                100, 50, "DIAMOND", "I", 75, 2475L, null
        );

        LeagueSummonerHistoryEntity entity2 = createHistoryEntity(
                2L, "puuid-2", "RANKED_SOLO_5x5", "league-2",
                80, 60, "PLATINUM", "II", 50, 2000L, null
        );

        List<LeagueSummonerHistoryEntity> entities = List.of(entity1, entity2);

        // when
        List<LeagueHistory> result = mapper.toDomainHistoryList(entities);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).puuid()).isEqualTo("puuid-1");
        assertThat(result.get(0).tier()).isEqualTo("DIAMOND");
        assertThat(result.get(1).puuid()).isEqualTo("puuid-2");
        assertThat(result.get(1).tier()).isEqualTo("PLATINUM");
    }

    @DisplayName("빈 히스토리 리스트 변환시 빈 리스트를 반환한다")
    @Test
    void toDomainHistoryList_emptyList_returnsEmptyList() {
        // given
        List<LeagueSummonerHistoryEntity> entities = List.of();

        // when
        List<LeagueHistory> result = mapper.toDomainHistoryList(entities);

        // then
        assertThat(result).isEmpty();
    }

    /**
     * LeagueSummonerHistoryEntity는 빌더가 private이므로 리플렉션을 사용해 생성
     */
    private LeagueSummonerHistoryEntity createHistoryEntity(
            Long leagueSummonerId, String puuid, String queue, String leagueId,
            int wins, int losses, String tier, String rank, int leaguePoints,
            long absolutePoints, LocalDateTime createdAt) throws Exception {

        LeagueSummonerHistoryEntity entity = LeagueSummonerHistoryEntity.class
                .getDeclaredConstructor()
                .newInstance();

        setField(entity, "leagueSummonerId", leagueSummonerId);
        setField(entity, "puuid", puuid);
        setField(entity, "queue", queue);
        setField(entity, "leagueId", leagueId);
        setField(entity, "wins", wins);
        setField(entity, "losses", losses);
        setField(entity, "tier", tier);
        setField(entity, "rank", rank);
        setField(entity, "leaguePoints", leaguePoints);
        setField(entity, "absolutePoints", absolutePoints);
        if (createdAt != null) {
            setField(entity, "createdAt", createdAt);
        }

        return entity;
    }

    private void setField(Object obj, String fieldName, Object value) throws Exception {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }
}
