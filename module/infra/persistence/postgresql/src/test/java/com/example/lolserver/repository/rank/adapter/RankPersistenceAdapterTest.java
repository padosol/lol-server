package com.example.lolserver.repository.rank.adapter;

import com.example.lolserver.Division;
import com.example.lolserver.QueueType;
import com.example.lolserver.Tier;
import com.example.lolserver.domain.rank.application.dto.RankSearchDto;
import com.example.lolserver.domain.rank.domain.Rank;
import com.example.lolserver.repository.config.RepositoryTestBase;
import com.example.lolserver.repository.rank.RankRepository;
import com.example.lolserver.repository.rank.entity.RankEntity;
import com.example.lolserver.repository.rank.mapper.RankMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RankPersistenceAdapterTest extends RepositoryTestBase {

    @Autowired
    private RankRepository rankRepository;

    @Autowired
    private RankMapper rankMapper;

    private RankPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new RankPersistenceAdapter(rankRepository, rankMapper);
    }

    @DisplayName("SOLO 타입으로 랭크를 조회하면 솔로랭크 도메인 객체를 반환한다")
    @Test
    void getRanks_soloType_returnsSoloRanks() {
        // given
        RankEntity soloRank = createRankEntity("Player1", QueueType.RANKED_SOLO_5x5, Tier.DIAMOND, Division.I);
        RankEntity flexRank = createRankEntity("Player2", QueueType.RANKED_FLEX_SR, Tier.PLATINUM, Division.II);
        rankRepository.saveAll(List.of(soloRank, flexRank));

        RankSearchDto searchDto = new RankSearchDto();
        searchDto.setType(RankSearchDto.GameType.SOLO);

        // when
        List<Rank> result = adapter.getRanks(searchDto);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSummonerName()).isEqualTo("Player1");
        assertThat(result.get(0).getTier()).isEqualTo("DIAMOND I");
    }

    @DisplayName("FLEX 타입으로 랭크를 조회하면 자유랭크 도메인 객체를 반환한다")
    @Test
    void getRanks_flexType_returnsFlexRanks() {
        // given
        RankEntity soloRank = createRankEntity("SoloPlayer", QueueType.RANKED_SOLO_5x5, Tier.MASTER, Division.I);
        RankEntity flexRank = createRankEntity("FlexPlayer", QueueType.RANKED_FLEX_SR, Tier.GOLD, Division.III);
        rankRepository.saveAll(List.of(soloRank, flexRank));

        RankSearchDto searchDto = new RankSearchDto();
        searchDto.setType(RankSearchDto.GameType.FLEX);

        // when
        List<Rank> result = adapter.getRanks(searchDto);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSummonerName()).isEqualTo("FlexPlayer");
        assertThat(result.get(0).getTier()).isEqualTo("GOLD III");
    }

    private RankEntity createRankEntity(String summonerName, QueueType queueType, Tier tier, Division division) {
        RankEntity entity = new RankEntity();
        entity.setQueueType(queueType);
        entity.setSummonerName(summonerName);
        entity.setTagLine("KR1");
        entity.setSummonerId("summoner-" + summonerName);
        entity.setLeagueId("league-" + summonerName);
        entity.setWin(100);
        entity.setLosses(50);
        entity.setPoint(75);
        entity.setTier(tier);
        entity.setDivision(division);
        entity.setPuuid("puuid-" + summonerName);
        entity.setSummonerLevel(300L);
        entity.setPosition("TOP");
        entity.setChampionNames("Garen,Darius");
        return entity;
    }
}
