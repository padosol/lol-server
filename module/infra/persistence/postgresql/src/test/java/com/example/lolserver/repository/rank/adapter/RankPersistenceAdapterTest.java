package com.example.lolserver.repository.rank.adapter;

import com.example.lolserver.domain.rank.application.dto.RankSearchDto;
import com.example.lolserver.domain.rank.domain.Rank;
import com.example.lolserver.repository.config.RepositoryTestBase;
import com.example.lolserver.repository.rank.SummonerRankingRepository;
import com.example.lolserver.repository.rank.entity.SummonerRankingEntity;
import com.example.lolserver.repository.rank.mapper.RankMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RankPersistenceAdapterTest extends RepositoryTestBase {

    @Autowired
    private SummonerRankingRepository summonerRankingRepository;

    @Autowired
    private RankMapper rankMapper;

    private RankPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new RankPersistenceAdapter(summonerRankingRepository, rankMapper);
    }

    @DisplayName("SOLO 타입으로 랭크를 조회하면 솔로랭크 도메인 객체를 반환한다")
    @Test
    void getRanks_soloType_returnsSoloRanks() {
        // given
        SummonerRankingEntity soloRank = createRankingEntity("Player1", "RANKED_SOLO_5x5", "DIAMOND", "I");
        SummonerRankingEntity flexRank = createRankingEntity("Player2", "RANKED_FLEX_SR", "PLATINUM", "II");
        summonerRankingRepository.saveAll(List.of(soloRank, flexRank));

        RankSearchDto searchDto = new RankSearchDto();
        searchDto.setRankType(RankSearchDto.GameType.SOLO);

        // when
        List<Rank> result = adapter.getRanks(searchDto);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getGameName()).isEqualTo("Player1");
        assertThat(result.get(0).getTier()).isEqualTo("DIAMOND I");
    }

    @DisplayName("FLEX 타입으로 랭크를 조회하면 자유랭크 도메인 객체를 반환한다")
    @Test
    void getRanks_flexType_returnsFlexRanks() {
        // given
        SummonerRankingEntity soloRank = createRankingEntity("SoloPlayer", "RANKED_SOLO_5x5", "MASTER", "I");
        SummonerRankingEntity flexRank = createRankingEntity("FlexPlayer", "RANKED_FLEX_SR", "GOLD", "III");
        summonerRankingRepository.saveAll(List.of(soloRank, flexRank));

        RankSearchDto searchDto = new RankSearchDto();
        searchDto.setRankType(RankSearchDto.GameType.FLEX);

        // when
        List<Rank> result = adapter.getRanks(searchDto);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getGameName()).isEqualTo("FlexPlayer");
        assertThat(result.get(0).getTier()).isEqualTo("GOLD III");
    }

    private SummonerRankingEntity createRankingEntity(String gameName, String queue, String tier, String rank) {
        return SummonerRankingEntity.builder()
                .puuid("puuid-" + gameName)
                .queue(queue)
                .currentRank(1)
                .rankChange(0)
                .gameName(gameName)
                .tagLine("KR1")
                .mostChampion1("Garen")
                .mostChampion2("Darius")
                .wins(100)
                .losses(50)
                .winRate(new BigDecimal("66.67"))
                .tier(tier)
                .rank(rank)
                .leaguePoints(75)
                .build();
    }
}
