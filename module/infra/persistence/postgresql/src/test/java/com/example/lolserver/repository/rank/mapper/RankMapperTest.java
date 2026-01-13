package com.example.lolserver.repository.rank.mapper;

import com.example.lolserver.Division;
import com.example.lolserver.QueueType;
import com.example.lolserver.Tier;
import com.example.lolserver.domain.rank.domain.Rank;
import com.example.lolserver.repository.rank.entity.RankEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RankMapperTest {

    private final RankMapper rankMapper = RankMapper.INSTANCE;

    @DisplayName("RankEntity를 Rank 도메인으로 변환한다")
    @Test
    void entityToDomain_validEntity_returnsRank() {
        // given
        RankEntity entity = new RankEntity();
        entity.setSummonerName("TestPlayer");
        entity.setTagLine("KR1");
        entity.setWin(100);
        entity.setLosses(50);
        entity.setPoint(75);
        entity.setTier(Tier.DIAMOND);
        entity.setDivision(Division.I);
        entity.setSummonerLevel(300L);
        entity.setPosition("TOP");
        entity.setChampionNames("Garen,Darius,Fiora");

        // when
        Rank result = rankMapper.entityToDomain(entity);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getSummonerName()).isEqualTo("TestPlayer");
        assertThat(result.getTagLine()).isEqualTo("KR1");
        assertThat(result.getWin()).isEqualTo(100);
        assertThat(result.getLosses()).isEqualTo(50);
        assertThat(result.getPoint()).isEqualTo(75);
        assertThat(result.getTier()).isEqualTo("DIAMOND I");
        assertThat(result.getSummonerLevel()).isEqualTo(300L);
        assertThat(result.getPosition()).isEqualTo("TOP");
        assertThat(result.getChampions()).containsExactly("Garen", "Darius", "Fiora");
    }

    @DisplayName("챔피언 이름이 null인 경우 빈 리스트를 반환한다")
    @Test
    void mapChampionNames_nullInput_returnsEmptyList() {
        // given
        String championNames = null;

        // when
        List<String> result = rankMapper.mapChampionNames(championNames);

        // then
        assertThat(result).isEmpty();
    }

    @DisplayName("챔피언 이름이 빈 문자열인 경우 빈 리스트를 반환한다")
    @Test
    void mapChampionNames_emptyString_returnsEmptyList() {
        // given
        String championNames = "";

        // when
        List<String> result = rankMapper.mapChampionNames(championNames);

        // then
        assertThat(result).isEmpty();
    }

    @DisplayName("쉼표로 구분된 챔피언 이름을 리스트로 변환한다")
    @Test
    void mapChampionNames_commaSeparatedString_returnsList() {
        // given
        String championNames = "Yasuo,Zed,Akali";

        // when
        List<String> result = rankMapper.mapChampionNames(championNames);

        // then
        assertThat(result).containsExactly("Yasuo", "Zed", "Akali");
    }

    @DisplayName("단일 챔피언 이름을 리스트로 변환한다")
    @Test
    void mapChampionNames_singleChampion_returnsSingleItemList() {
        // given
        String championNames = "Garen";

        // when
        List<String> result = rankMapper.mapChampionNames(championNames);

        // then
        assertThat(result).containsExactly("Garen");
    }

    @DisplayName("MASTER 티어와 Division I의 조합을 올바르게 변환한다")
    @Test
    void entityToDomain_masterTier_returnsMasterI() {
        // given
        RankEntity entity = new RankEntity();
        entity.setSummonerName("MasterPlayer");
        entity.setTagLine("KR1");
        entity.setTier(Tier.MASTER);
        entity.setDivision(Division.I);
        entity.setChampionNames("");

        // when
        Rank result = rankMapper.entityToDomain(entity);

        // then
        assertThat(result.getTier()).isEqualTo("MASTER I");
    }

    @DisplayName("GRANDMASTER 티어를 올바르게 변환한다")
    @Test
    void entityToDomain_grandmasterTier_returnsGrandmasterI() {
        // given
        RankEntity entity = new RankEntity();
        entity.setSummonerName("GMPlayer");
        entity.setTagLine("KR1");
        entity.setTier(Tier.GRANDMASTER);
        entity.setDivision(Division.I);
        entity.setChampionNames("");

        // when
        Rank result = rankMapper.entityToDomain(entity);

        // then
        assertThat(result.getTier()).isEqualTo("GRANDMASTER I");
    }
}
