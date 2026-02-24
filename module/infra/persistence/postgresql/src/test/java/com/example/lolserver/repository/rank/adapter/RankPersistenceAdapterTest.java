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
import org.springframework.data.domain.Page;

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

    @DisplayName("SOLO 타입으로 랭크를 조회하면 솔로랭크 도메인 객체를 페이지로 반환한다")
    @Test
    void getRanks_soloType_returnsSoloRanks() {
        // given
        SummonerRankingEntity soloRank = createRankingEntity("Player1", "RANKED_SOLO_5x5", "DIAMOND", "I", 1);
        SummonerRankingEntity flexRank = createRankingEntity("Player2", "RANKED_FLEX_SR", "PLATINUM", "II", 1);
        summonerRankingRepository.saveAll(List.of(soloRank, flexRank));

        String platformId = "kr";
        RankSearchDto searchDto = new RankSearchDto();
        searchDto.setRankType(RankSearchDto.GameType.SOLO);
        searchDto.setPage(1);

        // when
        Page<Rank> result = adapter.getRanks(searchDto, platformId);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getGameName()).isEqualTo("Player1");
        assertThat(result.getContent().get(0).getTier()).isEqualTo("DIAMOND");
        assertThat(result.getContent().get(0).getRank()).isEqualTo("I");
    }

    @DisplayName("FLEX 타입으로 랭크를 조회하면 자유랭크 도메인 객체를 페이지로 반환한다")
    @Test
    void getRanks_flexType_returnsFlexRanks() {
        // given
        SummonerRankingEntity soloRank = createRankingEntity("SoloPlayer", "RANKED_SOLO_5x5", "MASTER", null, 1);
        SummonerRankingEntity flexRank = createRankingEntity("FlexPlayer", "RANKED_FLEX_SR", "GOLD", "III", 1);
        summonerRankingRepository.saveAll(List.of(soloRank, flexRank));

        String platformId = "kr";
        RankSearchDto searchDto = new RankSearchDto();
        searchDto.setRankType(RankSearchDto.GameType.FLEX);
        searchDto.setPage(1);

        // when
        Page<Rank> result = adapter.getRanks(searchDto, platformId);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getGameName()).isEqualTo("FlexPlayer");
        assertThat(result.getContent().get(0).getTier()).isEqualTo("GOLD");
        assertThat(result.getContent().get(0).getRank()).isEqualTo("III");
    }

    @DisplayName("tier 필터링을 적용하면 해당 티어만 반환한다")
    @Test
    void getRanks_withTierFilter_returnsFilteredRanks() {
        // given
        SummonerRankingEntity diamondPlayer = createRankingEntity("DiamondPlayer", "RANKED_SOLO_5x5", "DIAMOND", "I", 1);
        SummonerRankingEntity goldPlayer = createRankingEntity("GoldPlayer", "RANKED_SOLO_5x5", "GOLD", "II", 2);
        summonerRankingRepository.saveAll(List.of(diamondPlayer, goldPlayer));

        String platformId = "kr";
        RankSearchDto searchDto = new RankSearchDto();
        searchDto.setRankType(RankSearchDto.GameType.SOLO);
        searchDto.setTier("DIAMOND");
        searchDto.setPage(1);

        // when
        Page<Rank> result = adapter.getRanks(searchDto, platformId);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTier()).isEqualTo("DIAMOND");
    }

    @DisplayName("페이징이 currentRank 기준으로 정렬된다")
    @Test
    void getRanks_sortedByCurrentRank() {
        // given
        SummonerRankingEntity rank2 = createRankingEntity("Player2", "RANKED_SOLO_5x5", "DIAMOND", "I", 2);
        SummonerRankingEntity rank1 = createRankingEntity("Player1", "RANKED_SOLO_5x5", "DIAMOND", "I", 1);
        SummonerRankingEntity rank3 = createRankingEntity("Player3", "RANKED_SOLO_5x5", "DIAMOND", "I", 3);
        summonerRankingRepository.saveAll(List.of(rank2, rank1, rank3));

        String platformId = "kr";
        RankSearchDto searchDto = new RankSearchDto();
        searchDto.setRankType(RankSearchDto.GameType.SOLO);
        searchDto.setPage(1);

        // when
        Page<Rank> result = adapter.getRanks(searchDto, platformId);

        // then
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getContent().get(0).getCurrentRank()).isEqualTo(1);
        assertThat(result.getContent().get(1).getCurrentRank()).isEqualTo(2);
        assertThat(result.getContent().get(2).getCurrentRank()).isEqualTo(3);
    }

    private SummonerRankingEntity createRankingEntity(String gameName, String queue, String tier, String rank, int currentRank) {
        return SummonerRankingEntity.builder()
                .puuid("puuid-" + gameName)
                .queue(queue)
                .platformId("KR")
                .currentRank(currentRank)
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
