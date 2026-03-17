# 같은 Tier 내 게임수 내림차순 정렬

## Context
현재 `ChampionTierCalculator.assignTiers()`의 재정렬 로직(75~79번 줄)에서 같은 tier 내 정렬 기준이 **score 내림차순**으로 되어 있다. 사용자 요청에 따라 같은 tier 내에서는 **totalGames 내림차순**으로 정렬하도록 변경한다.

## 변경 사항

### `ChampionTierCalculator.java` (1줄 수정)
경로: `module/core/lol-server-domain/src/main/java/.../championstats/application/ChampionTierCalculator.java` 78번 줄

```java
// 현재 (score 내림차순)
return Double.compare(scores[b], scores[a]);

// 변경 (totalGames 내림차순)
return Long.compare(champions.get(b).totalGames(), champions.get(a).totalGames());
```

### `ChampionTierCalculatorTest.java` (테스트 추가)
경로: `module/core/lol-server-domain/src/test/java/.../championstats/application/ChampionTierCalculatorTest.java`

같은 tier 내에서 게임수가 많은 챔피언이 먼저 오는지 검증하는 테스트 추가:

```java
@DisplayName("같은 tier 내에서 게임수가 많은 챔피언이 먼저 정렬된다")
@Test
void sameTier_sortedByTotalGamesDescending() {
    List<ChampionRateReadModel> input = List.of(
        new ChampionRateReadModel(1, 0.50, 0.05, 0.03, 800),   // 게임수 적음
        new ChampionRateReadModel(2, 0.50, 0.05, 0.03, 2000),  // 게임수 많음
        new ChampionRateReadModel(3, 0.50, 0.05, 0.03, 1200)   // 게임수 중간
    );

    List<ChampionRateReadModel> result = ChampionTierCalculator.assignTiers(input);

    // 모두 같은 tier이므로 게임수 내림차순으로 정렬되어야 함
    assertThat(result.get(0).totalGames()).isGreaterThanOrEqualTo(result.get(1).totalGames());
    assertThat(result.get(1).totalGames()).isGreaterThanOrEqualTo(result.get(2).totalGames());
}
```

## 검증
- `./gradlew :module:core:lol-server-domain:test --tests "...ChampionTierCalculatorTest"` 통과 확인
- `./gradlew test` 전체 테스트 통과 확인
