package com.example.lolserver.domain.championstats.application;

import com.example.lolserver.domain.championstats.application.model.ChampionRateReadModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class ChampionTierCalculatorTest {

    @DisplayName("빈 리스트를 입력하면 빈 리스트를 반환한다")
    @Test
    void emptyList_returnsEmpty() {
        List<ChampionRateReadModel> result = ChampionTierCalculator.assignTiers(List.of());
        assertThat(result).isEmpty();
    }

    @DisplayName("단일 챔피언은 OP tier를 받는다")
    @Test
    void singleChampion_getsOpTier() {
        List<ChampionRateReadModel> input = List.of(
                new ChampionRateReadModel(266, 0.52, 0.08, 0.05, 1500)
        );

        List<ChampionRateReadModel> result = ChampionTierCalculator.assignTiers(input);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).tier()).isEqualTo("OP");
        assertThat(result.get(0).championId()).isEqualTo(266);
    }

    @DisplayName("totalGames가 50 미만이면 자동으로 tier 5가 된다")
    @Test
    void lowGames_getsTier5() {
        List<ChampionRateReadModel> input = List.of(
                new ChampionRateReadModel(1, 0.70, 0.15, 0.10, 49),
                new ChampionRateReadModel(2, 0.40, 0.02, 0.01, 1500)
        );

        List<ChampionRateReadModel> result = ChampionTierCalculator.assignTiers(input);

        ChampionRateReadModel lowGamesChampion = result.stream()
                .filter(c -> c.championId() == 1).findFirst().orElseThrow();
        assertThat(lowGamesChampion.tier()).isEqualTo("5");
    }

    @DisplayName("다수 챔피언에 대해 백분위 기반으로 tier를 올바르게 배정한다")
    @Test
    void multipleChampions_assignsTiersByPercentile() {
        List<ChampionRateReadModel> input = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            double winRate = 0.40 + (i * 0.002);
            double pickRate = 0.01 + (i * 0.001);
            double banRate = 0.005 + (i * 0.0005);
            input.add(new ChampionRateReadModel(i + 1, winRate, pickRate, banRate, 1000 + i * 10));
        }

        List<ChampionRateReadModel> result = ChampionTierCalculator.assignTiers(input);

        assertThat(result).hasSize(100);

        Map<String, Long> tierCounts = result.stream()
                .collect(Collectors.groupingBy(ChampionRateReadModel::tier, Collectors.counting()));

        assertThat(tierCounts.getOrDefault("OP", 0L)).isEqualTo(3);   // < 3%
        assertThat(tierCounts.getOrDefault("1", 0L)).isEqualTo(7);   // 3% ~ 10%
        assertThat(tierCounts.getOrDefault("2", 0L)).isEqualTo(15);  // 10% ~ 25%
        assertThat(tierCounts.getOrDefault("3", 0L)).isEqualTo(25);  // 25% ~ 50%
        assertThat(tierCounts.getOrDefault("4", 0L)).isEqualTo(25);  // 50% ~ 75%
        assertThat(tierCounts.getOrDefault("5", 0L)).isEqualTo(25);  // 75% ~
    }

    @DisplayName("승률과 픽률이 높은 챔피언이 더 높은 tier를 받는다")
    @Test
    void highWinRateAndPickRate_getsHigherTier() {
        List<ChampionRateReadModel> input = List.of(
                new ChampionRateReadModel(1, 0.55, 0.15, 0.10, 2000),
                new ChampionRateReadModel(2, 0.52, 0.10, 0.07, 1500),
                new ChampionRateReadModel(3, 0.48, 0.05, 0.03, 1000),
                new ChampionRateReadModel(4, 0.45, 0.03, 0.01, 800)
        );

        List<ChampionRateReadModel> result = ChampionTierCalculator.assignTiers(input);

        ChampionRateReadModel best = result.stream()
                .filter(c -> c.championId() == 1).findFirst().orElseThrow();
        ChampionRateReadModel worst = result.stream()
                .filter(c -> c.championId() == 4).findFirst().orElseThrow();

        assertThat(tierRank(best.tier())).isLessThan(tierRank(worst.tier()));

        // 결과가 tier 내림차순(OP→1→2→...→5)으로 정렬되어 있는지 검증
        for (int i = 0; i < result.size() - 1; i++) {
            assertThat(tierRank(result.get(i).tier()))
                    .isLessThanOrEqualTo(tierRank(result.get(i + 1).tier()));
        }
    }

    @DisplayName("원본 데이터(championId, winRate 등)가 보존된다")
    @Test
    void originalDataIsPreserved() {
        List<ChampionRateReadModel> input = List.of(
                new ChampionRateReadModel(266, 0.52, 0.08, 0.05, 1500)
        );

        List<ChampionRateReadModel> result = ChampionTierCalculator.assignTiers(input);

        ChampionRateReadModel champion = result.get(0);
        assertThat(champion.championId()).isEqualTo(266);
        assertThat(champion.winRate()).isEqualTo(0.52);
        assertThat(champion.pickRate()).isEqualTo(0.08);
        assertThat(champion.banRate()).isEqualTo(0.05);
        assertThat(champion.totalGames()).isEqualTo(1500);
        assertThat(champion.tier()).isNotNull();
    }

    @DisplayName("게임수 부족 챔피언은 score가 높아도 tier 5로 배정되어 뒤쪽에 위치한다")
    @Test
    void lowGamesHighScore_sortedAfterHigherTiers() {
        List<ChampionRateReadModel> input = List.of(
                new ChampionRateReadModel(1, 0.70, 0.20, 0.15, 30),
                new ChampionRateReadModel(2, 0.52, 0.10, 0.07, 1500),
                new ChampionRateReadModel(3, 0.48, 0.05, 0.03, 1000),
                new ChampionRateReadModel(4, 0.45, 0.03, 0.01, 800)
        );

        List<ChampionRateReadModel> result = ChampionTierCalculator.assignTiers(input);

        for (int i = 0; i < result.size() - 1; i++) {
            assertThat(tierRank(result.get(i).tier()))
                    .isLessThanOrEqualTo(tierRank(result.get(i + 1).tier()));
        }
        // 게임수 부족 챔피언(id=1)은 tier 5로 배정
        ChampionRateReadModel lowGames = result.stream()
                .filter(c -> c.championId() == 1).findFirst().orElseThrow();
        assertThat(lowGames.tier()).isEqualTo("5");
    }

    @DisplayName("같은 tier 내에서 게임수가 많은 챔피언이 먼저 정렬된다")
    @Test
    void sameTier_sortedByTotalGamesDescending() {
        List<ChampionRateReadModel> input = List.of(
                new ChampionRateReadModel(1, 0.50, 0.05, 0.03, 10),
                new ChampionRateReadModel(2, 0.50, 0.05, 0.03, 40),
                new ChampionRateReadModel(3, 0.50, 0.05, 0.03, 25)
        );

        List<ChampionRateReadModel> result = ChampionTierCalculator.assignTiers(input);

        // 모두 게임수 50 미만이므로 같은 tier 5, 게임수 내림차순으로 정렬되어야 함
        assertThat(result).allMatch(c -> c.tier().equals("5"));
        assertThat(result.get(0).totalGames()).isGreaterThanOrEqualTo(result.get(1).totalGames());
        assertThat(result.get(1).totalGames()).isGreaterThanOrEqualTo(result.get(2).totalGames());
    }

    private int tierRank(String tier) {
        return switch (tier) {
            case "OP" -> 0;
            case "1" -> 1;
            case "2" -> 2;
            case "3" -> 3;
            case "4" -> 4;
            case "5" -> 5;
            default -> throw new IllegalArgumentException("Unknown tier: " + tier);
        };
    }
}
