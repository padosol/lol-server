package com.example.lolserver.repository.rank;

import com.example.lolserver.Division;
import com.example.lolserver.QueueType;
import com.example.lolserver.Tier;
import com.example.lolserver.repository.config.RepositoryTestBase;
import com.example.lolserver.repository.rank.entity.RankEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class RankRepositoryTest extends RepositoryTestBase {

    @Autowired
    private RankRepository rankRepository;

    @DisplayName("랭크 정보를 저장하고 ID로 조회한다")
    @Test
    void save_validRank_findById() {
        // given
        RankEntity rank = new RankEntity();
        rank.setQueueType(QueueType.RANKED_SOLO_5x5);
        rank.setSummonerName("TestPlayer");
        rank.setTagLine("KR1");
        rank.setSummonerId("summoner-123");
        rank.setLeagueId("league-456");
        rank.setWin(100);
        rank.setLosses(50);
        rank.setPoint(75);
        rank.setTier(Tier.DIAMOND);
        rank.setDivision(Division.I);
        rank.setPuuid("puuid-789");
        rank.setSummonerLevel(300L);
        rank.setPosition("TOP");
        rank.setChampionNames("Garen,Darius,Sett");

        // when
        RankEntity saved = rankRepository.save(rank);
        Optional<RankEntity> found = rankRepository.findById(saved.getId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getSummonerName()).isEqualTo("TestPlayer");
        assertThat(found.get().getTier()).isEqualTo(Tier.DIAMOND);
        assertThat(found.get().getDivision()).isEqualTo(Division.I);
        assertThat(found.get().getQueueType()).isEqualTo(QueueType.RANKED_SOLO_5x5);
    }

    @DisplayName("큐 타입으로 랭크 목록을 조회한다")
    @Test
    void findByQueueType_validQueueType_returnsRanks() {
        // given
        RankEntity soloRank = new RankEntity();
        soloRank.setQueueType(QueueType.RANKED_SOLO_5x5);
        soloRank.setSummonerName("SoloPlayer");
        soloRank.setTagLine("KR1");
        soloRank.setSummonerId("summoner-solo");
        soloRank.setLeagueId("league-solo");
        soloRank.setWin(80);
        soloRank.setLosses(40);
        soloRank.setPoint(50);
        soloRank.setTier(Tier.PLATINUM);
        soloRank.setDivision(Division.II);
        soloRank.setPuuid("puuid-solo");
        soloRank.setSummonerLevel(250L);
        soloRank.setPosition("MID");
        soloRank.setChampionNames("Ahri,Zed");

        RankEntity flexRank = new RankEntity();
        flexRank.setQueueType(QueueType.RANKED_FLEX_SR);
        flexRank.setSummonerName("FlexPlayer");
        flexRank.setTagLine("KR1");
        flexRank.setSummonerId("summoner-flex");
        flexRank.setLeagueId("league-flex");
        flexRank.setWin(60);
        flexRank.setLosses(30);
        flexRank.setPoint(25);
        flexRank.setTier(Tier.GOLD);
        flexRank.setDivision(Division.III);
        flexRank.setPuuid("puuid-flex");
        flexRank.setSummonerLevel(200L);
        flexRank.setPosition("SUPPORT");
        flexRank.setChampionNames("Lulu,Nami");

        rankRepository.saveAll(List.of(soloRank, flexRank));

        // when
        List<RankEntity> result = rankRepository.findByQueueType(QueueType.RANKED_SOLO_5x5);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSummonerName()).isEqualTo("SoloPlayer");
        assertThat(result.get(0).getQueueType()).isEqualTo(QueueType.RANKED_SOLO_5x5);
    }

    @DisplayName("여러 랭크를 저장하고 전체 목록을 조회한다")
    @Test
    void findAll_multipleRanks_returnsAll() {
        // given
        RankEntity rank1 = new RankEntity();
        rank1.setQueueType(QueueType.RANKED_SOLO_5x5);
        rank1.setSummonerName("Player1");
        rank1.setTagLine("KR1");
        rank1.setSummonerId("summoner-1");
        rank1.setLeagueId("league-1");
        rank1.setWin(100);
        rank1.setLosses(50);
        rank1.setPoint(75);
        rank1.setTier(Tier.CHALLENGER);
        rank1.setDivision(Division.I);
        rank1.setPuuid("puuid-1");
        rank1.setSummonerLevel(500L);
        rank1.setPosition("ADC");
        rank1.setChampionNames("Jinx,Kaisa");

        RankEntity rank2 = new RankEntity();
        rank2.setQueueType(QueueType.RANKED_SOLO_5x5);
        rank2.setSummonerName("Player2");
        rank2.setTagLine("KR1");
        rank2.setSummonerId("summoner-2");
        rank2.setLeagueId("league-2");
        rank2.setWin(90);
        rank2.setLosses(60);
        rank2.setPoint(30);
        rank2.setTier(Tier.GRANDMASTER);
        rank2.setDivision(Division.I);
        rank2.setPuuid("puuid-2");
        rank2.setSummonerLevel(450L);
        rank2.setPosition("JUNGLE");
        rank2.setChampionNames("LeeSin,Elise");

        rankRepository.saveAll(List.of(rank1, rank2));

        // when
        List<RankEntity> result = rankRepository.findAll();

        // then
        assertThat(result).hasSize(2);
    }
}
