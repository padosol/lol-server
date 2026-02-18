package com.example.lolserver.repository.rank;

import com.example.lolserver.repository.config.RepositoryTestBase;
import com.example.lolserver.repository.rank.entity.SummonerRankingEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class SummonerRankingRepositoryTest extends RepositoryTestBase {

    @Autowired
    private SummonerRankingRepository summonerRankingRepository;

    @DisplayName("소환사 랭킹 정보를 저장하고 ID로 조회한다")
    @Test
    void save_validRanking_findById() {
        // given
        SummonerRankingEntity ranking = SummonerRankingEntity.builder()
                .puuid("puuid-789")
                .queue("RANKED_SOLO_5x5")
                .region("KR")
                .currentRank(1)
                .rankChange(2)
                .gameName("TestPlayer")
                .tagLine("KR1")
                .mostChampion1("Garen")
                .mostChampion2("Darius")
                .mostChampion3("Sett")
                .wins(100)
                .losses(50)
                .winRate(new BigDecimal("66.67"))
                .tier("DIAMOND")
                .rank("I")
                .leaguePoints(75)
                .build();

        // when
        SummonerRankingEntity saved = summonerRankingRepository.save(ranking);
        Optional<SummonerRankingEntity> found = summonerRankingRepository.findById(saved.getId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getGameName()).isEqualTo("TestPlayer");
        assertThat(found.get().getTier()).isEqualTo("DIAMOND");
        assertThat(found.get().getRank()).isEqualTo("I");
        assertThat(found.get().getQueue()).isEqualTo("RANKED_SOLO_5x5");
    }

    @DisplayName("큐 타입으로 소환사 랭킹 목록을 조회한다")
    @Test
    void findByQueue_validQueue_returnsRankings() {
        // given
        SummonerRankingEntity soloRanking = SummonerRankingEntity.builder()
                .puuid("puuid-solo")
                .queue("RANKED_SOLO_5x5")
                .region("KR")
                .currentRank(1)
                .rankChange(0)
                .gameName("SoloPlayer")
                .tagLine("KR1")
                .mostChampion1("Ahri")
                .mostChampion2("Zed")
                .wins(80)
                .losses(40)
                .winRate(new BigDecimal("66.67"))
                .tier("PLATINUM")
                .rank("II")
                .leaguePoints(50)
                .build();

        SummonerRankingEntity flexRanking = SummonerRankingEntity.builder()
                .puuid("puuid-flex")
                .queue("RANKED_FLEX_SR")
                .region("KR")
                .currentRank(1)
                .rankChange(1)
                .gameName("FlexPlayer")
                .tagLine("KR1")
                .mostChampion1("Lulu")
                .mostChampion2("Nami")
                .wins(60)
                .losses(30)
                .winRate(new BigDecimal("66.67"))
                .tier("GOLD")
                .rank("III")
                .leaguePoints(25)
                .build();

        summonerRankingRepository.saveAll(List.of(soloRanking, flexRanking));

        // when
        List<SummonerRankingEntity> result = summonerRankingRepository.findByQueue("RANKED_SOLO_5x5");

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getGameName()).isEqualTo("SoloPlayer");
        assertThat(result.get(0).getQueue()).isEqualTo("RANKED_SOLO_5x5");
    }

    @DisplayName("여러 소환사 랭킹을 저장하고 전체 목록을 조회한다")
    @Test
    void findAll_multipleRankings_returnsAll() {
        // given
        SummonerRankingEntity ranking1 = SummonerRankingEntity.builder()
                .puuid("puuid-1")
                .queue("RANKED_SOLO_5x5")
                .region("KR")
                .currentRank(1)
                .rankChange(0)
                .gameName("Player1")
                .tagLine("KR1")
                .mostChampion1("Jinx")
                .mostChampion2("Kaisa")
                .wins(100)
                .losses(50)
                .winRate(new BigDecimal("66.67"))
                .tier("CHALLENGER")
                .rank("I")
                .leaguePoints(75)
                .build();

        SummonerRankingEntity ranking2 = SummonerRankingEntity.builder()
                .puuid("puuid-2")
                .queue("RANKED_SOLO_5x5")
                .region("KR")
                .currentRank(2)
                .rankChange(-1)
                .gameName("Player2")
                .tagLine("KR1")
                .mostChampion1("LeeSin")
                .mostChampion2("Elise")
                .wins(90)
                .losses(60)
                .winRate(new BigDecimal("60.00"))
                .tier("GRANDMASTER")
                .rank("I")
                .leaguePoints(30)
                .build();

        summonerRankingRepository.saveAll(List.of(ranking1, ranking2));

        // when
        List<SummonerRankingEntity> result = summonerRankingRepository.findAll();

        // then
        assertThat(result).hasSize(2);
    }
}
