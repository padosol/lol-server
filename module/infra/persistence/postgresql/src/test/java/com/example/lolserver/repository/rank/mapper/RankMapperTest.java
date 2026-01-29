package com.example.lolserver.repository.rank.mapper;

import com.example.lolserver.domain.rank.domain.Rank;
import com.example.lolserver.repository.rank.entity.SummonerRankingEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class RankMapperTest {

    private final RankMapper rankMapper = RankMapper.INSTANCE;

    @DisplayName("SummonerRankingEntity를 Rank 도메인으로 변환한다")
    @Test
    void entityToDomain_validEntity_returnsRank() {
        // given
        SummonerRankingEntity entity = SummonerRankingEntity.builder()
                .puuid("puuid-test")
                .queue("RANKED_SOLO_5x5")
                .currentRank(1)
                .rankChange(2)
                .gameName("TestPlayer")
                .tagLine("KR1")
                .mostChampion1("Garen")
                .mostChampion2("Darius")
                .mostChampion3("Fiora")
                .wins(100)
                .losses(50)
                .winRate(new BigDecimal("66.67"))
                .tier("DIAMOND")
                .rank("I")
                .leaguePoints(75)
                .build();

        // when
        Rank result = rankMapper.entityToDomain(entity);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getPuuid()).isEqualTo("puuid-test");
        assertThat(result.getCurrentRank()).isEqualTo(1);
        assertThat(result.getRankChange()).isEqualTo(2);
        assertThat(result.getGameName()).isEqualTo("TestPlayer");
        assertThat(result.getTagLine()).isEqualTo("KR1");
        assertThat(result.getWins()).isEqualTo(100);
        assertThat(result.getLosses()).isEqualTo(50);
        assertThat(result.getWinRate()).isEqualTo(new BigDecimal("66.67"));
        assertThat(result.getTier()).isEqualTo("DIAMOND I");
        assertThat(result.getLeaguePoints()).isEqualTo(75);
        assertThat(result.getChampions()).containsExactly("Garen", "Darius", "Fiora");
    }

    @DisplayName("챔피언이 없는 경우 빈 리스트를 반환한다")
    @Test
    void mapChampions_noChampions_returnsEmptyList() {
        // given
        SummonerRankingEntity entity = SummonerRankingEntity.builder()
                .puuid("puuid-test")
                .queue("RANKED_SOLO_5x5")
                .currentRank(1)
                .rankChange(0)
                .gameName("TestPlayer")
                .tagLine("KR1")
                .wins(50)
                .losses(50)
                .winRate(new BigDecimal("50.00"))
                .tier("GOLD")
                .rank("IV")
                .leaguePoints(0)
                .build();

        // when
        Rank result = rankMapper.entityToDomain(entity);

        // then
        assertThat(result.getChampions()).isEmpty();
    }

    @DisplayName("일부 챔피언만 있는 경우 해당 챔피언만 반환한다")
    @Test
    void mapChampions_partialChampions_returnsPartialList() {
        // given
        SummonerRankingEntity entity = SummonerRankingEntity.builder()
                .puuid("puuid-test")
                .queue("RANKED_SOLO_5x5")
                .currentRank(1)
                .rankChange(0)
                .gameName("TestPlayer")
                .tagLine("KR1")
                .mostChampion1("Yasuo")
                .mostChampion2("Zed")
                .wins(100)
                .losses(50)
                .winRate(new BigDecimal("66.67"))
                .tier("PLATINUM")
                .rank("II")
                .leaguePoints(50)
                .build();

        // when
        Rank result = rankMapper.entityToDomain(entity);

        // then
        assertThat(result.getChampions()).containsExactly("Yasuo", "Zed");
    }

    @DisplayName("MASTER 티어는 rank 없이 티어만 반환한다")
    @Test
    void entityToDomain_masterTier_returnsMasterOnly() {
        // given
        SummonerRankingEntity entity = SummonerRankingEntity.builder()
                .puuid("puuid-master")
                .queue("RANKED_SOLO_5x5")
                .currentRank(1)
                .rankChange(0)
                .gameName("MasterPlayer")
                .tagLine("KR1")
                .wins(200)
                .losses(100)
                .winRate(new BigDecimal("66.67"))
                .tier("MASTER")
                .leaguePoints(300)
                .build();

        // when
        Rank result = rankMapper.entityToDomain(entity);

        // then
        assertThat(result.getTier()).isEqualTo("MASTER");
    }

    @DisplayName("GRANDMASTER 티어는 rank 없이 티어만 반환한다")
    @Test
    void entityToDomain_grandmasterTier_returnsGrandmasterOnly() {
        // given
        SummonerRankingEntity entity = SummonerRankingEntity.builder()
                .puuid("puuid-gm")
                .queue("RANKED_SOLO_5x5")
                .currentRank(1)
                .rankChange(0)
                .gameName("GMPlayer")
                .tagLine("KR1")
                .wins(300)
                .losses(100)
                .winRate(new BigDecimal("75.00"))
                .tier("GRANDMASTER")
                .leaguePoints(500)
                .build();

        // when
        Rank result = rankMapper.entityToDomain(entity);

        // then
        assertThat(result.getTier()).isEqualTo("GRANDMASTER");
    }

    @DisplayName("CHALLENGER 티어는 rank 없이 티어만 반환한다")
    @Test
    void entityToDomain_challengerTier_returnsChallengerOnly() {
        // given
        SummonerRankingEntity entity = SummonerRankingEntity.builder()
                .puuid("puuid-challenger")
                .queue("RANKED_SOLO_5x5")
                .currentRank(1)
                .rankChange(0)
                .gameName("ChallengerPlayer")
                .tagLine("KR1")
                .wins(500)
                .losses(200)
                .winRate(new BigDecimal("71.43"))
                .tier("CHALLENGER")
                .leaguePoints(1000)
                .build();

        // when
        Rank result = rankMapper.entityToDomain(entity);

        // then
        assertThat(result.getTier()).isEqualTo("CHALLENGER");
    }
}
