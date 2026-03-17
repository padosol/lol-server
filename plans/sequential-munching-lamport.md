# 챔피언 통계 API 구현 계획 (docs/04_queries.sql Section 2~7)

## Context

`docs/04_queries.sql`에 정의된 7개 섹션의 ClickHouse 쿼리 중 Section 1(포지션별 챔피언 승률/픽률/밴률)은 이미 구현되어 있다. Section 2~7(룬, 소환사 주문, 스킬빌드, 시작 아이템, 3코어 빌드, 완성 아이템)을 `*_agg` 테이블 기반으로 구현한다.

**설계 결정:**
- 기존 matchup/winRate 기능은 **유지** (`*_local` 테이블 쿼리 유지)
- API에 `position` 필수 파라미터 추가 → 특정 포지션 상세 통계 반환
- 새로운 통계 3종 추가: 소환사 주문, 시작 아이템, 완성 아이템

---

## Step 1: 새 ReadModel 추가 (core 계층)

경로: `module/core/lol-server-domain/src/main/java/com/example/lolserver/domain/championstats/application/model/`

### 1-1. 새 ReadModel 3개 생성

`ChampionSpellStatsReadModel.java`:
```java
public record ChampionSpellStatsReadModel(
    int summoner1Id, int summoner2Id,
    long games, double winRate, double pickRate
) {}
```

`ChampionStartItemBuildReadModel.java`:
```java
public record ChampionStartItemBuildReadModel(
    String startItems, long games, double winRate, double pickRate
) {}
```

`ChampionItemStatsReadModel.java`:
```java
public record ChampionItemStatsReadModel(
    int itemId, String itemName,
    long games, double winRate, double pickRate
) {}
```

### 1-2. 기존 ReadModel 교체

`ChampionRuneBuildReadModel.java` - 개별 perk 컬럼 + pickRate:
```java
public record ChampionRuneBuildReadModel(
    int primaryStyleId, int subStyleId,
    int primaryPerk0, int primaryPerk1, int primaryPerk2, int primaryPerk3,
    int subPerk0, int subPerk1,
    int statPerkDefense, int statPerkFlex, int statPerkOffense,
    long games, double winRate, double pickRate
) {}
```

`ChampionSkillBuildReadModel.java`:
```java
public record ChampionSkillBuildReadModel(
    String skillBuild, long games, double winRate, double pickRate
) {}
```

`ChampionItemBuildReadModel.java` (3코어 빌드 순서):
```java
public record ChampionItemBuildReadModel(
    String itemBuild, long games, double winRate, double pickRate
) {}
```

### 1-3. 복합 ReadModel 교체

`ChampionPositionStatsReadModel.java`:
```java
public record ChampionPositionStatsReadModel(
    String teamPosition,
    double winRate,
    long totalGames,
    List<ChampionMatchupReadModel> matchups,
    List<ChampionRuneBuildReadModel> runeBuilds,
    List<ChampionSpellStatsReadModel> spellStats,
    List<ChampionSkillBuildReadModel> skillBuilds,
    List<ChampionStartItemBuildReadModel> startItemBuilds,
    List<ChampionItemBuildReadModel> itemBuilds,
    Map<Integer, List<ChampionItemStatsReadModel>> itemStatsByOrder
) {}
```

`ChampionStatsReadModel.java`:
```java
public record ChampionStatsReadModel(
    String tier,
    ChampionPositionStatsReadModel stats  // List → 단일 객체
) {}
```

### 유지하는 ReadModel
- `ChampionMatchupReadModel` - 그대로 유지
- `ChampionWinRateReadModel` - 그대로 유지
- `ChampionRateReadModel` - 그대로 유지 (positions API용)
- `PositionChampionStatsReadModel` - 그대로 유지 (positions API용)

---

## Step 2: 포트 인터페이스 변경

파일: `module/core/lol-server-domain/src/main/java/com/example/lolserver/domain/championstats/application/port/out/ChampionStatsQueryPort.java`

```java
public interface ChampionStatsQueryPort {
    // 기존 유지 (positions API용)
    Map<String, List<ChampionRateReadModel>> getChampionStatsByPosition(
            String patch, String platformId, String tier);

    // 기존 유지 + position 파라미터 추가 (시그니처 변경)
    ChampionWinRateReadModel getChampionWinRate(
            int championId, String patch, String platformId, String tier, String position);
    List<ChampionMatchupReadModel> getChampionMatchups(
            int championId, String patch, String platformId, String tier, String position);

    // Section 2~7 신규
    List<ChampionRuneBuildReadModel> getChampionRuneBuilds(
            int championId, String patch, String platformId, String tier, String position);
    List<ChampionSpellStatsReadModel> getChampionSpellStats(
            int championId, String patch, String platformId, String tier, String position);
    List<ChampionSkillBuildReadModel> getChampionSkillBuilds(
            int championId, String patch, String platformId, String tier, String position);
    List<ChampionStartItemBuildReadModel> getChampionStartItemBuilds(
            int championId, String patch, String platformId, String tier, String position);
    List<ChampionItemBuildReadModel> getChampionItemBuilds(
            int championId, String patch, String platformId, String tier, String position);
    List<ChampionItemStatsReadModel> getChampionItemStats(
            int championId, String patch, String platformId, String tier, String position, int itemOrder);
}
```

**변경 요약:**
- `getChampionWinRates` → `getChampionWinRate` (복수→단수, position 파라미터 추가, 반환 List→단일)
- `getChampionMatchups` 시그니처 변경 (position 추가, Map→List 반환)
- 기존 `getChampionItemBuilds`, `getChampionRuneBuilds`, `getChampionSkillBuilds` 시그니처 변경 (position 추가, Map→List 반환)
- 3개 메서드 추가: `getChampionSpellStats`, `getChampionStartItemBuilds`, `getChampionItemStats`

---

## Step 3: 서비스 변경

파일: `module/core/lol-server-domain/src/main/java/com/example/lolserver/domain/championstats/application/ChampionStatsService.java`

```java
public ChampionStatsReadModel getChampionStats(
        int championId, String patch, String platformId, String tier, String position) {

    ChampionWinRateReadModel winRate = championStatsQueryPort.getChampionWinRate(...);
    List<ChampionMatchupReadModel> matchups = championStatsQueryPort.getChampionMatchups(...);
    List<ChampionRuneBuildReadModel> runeBuilds = championStatsQueryPort.getChampionRuneBuilds(...);
    List<ChampionSpellStatsReadModel> spellStats = championStatsQueryPort.getChampionSpellStats(...);
    List<ChampionSkillBuildReadModel> skillBuilds = championStatsQueryPort.getChampionSkillBuilds(...);
    List<ChampionStartItemBuildReadModel> startItemBuilds = championStatsQueryPort.getChampionStartItemBuilds(...);
    List<ChampionItemBuildReadModel> itemBuilds = championStatsQueryPort.getChampionItemBuilds(...);

    // Section 7: 1~3코어 각각 조회
    Map<Integer, List<ChampionItemStatsReadModel>> itemStatsByOrder = new LinkedHashMap<>();
    for (int order = 1; order <= 3; order++) {
        itemStatsByOrder.put(order, championStatsQueryPort.getChampionItemStats(..., order));
    }

    ChampionPositionStatsReadModel positionStats = new ChampionPositionStatsReadModel(
        position, winRate.totalWinRate(), winRate.totalGames(),
        matchups, runeBuilds, spellStats, skillBuilds,
        startItemBuilds, itemBuilds, itemStatsByOrder
    );

    return new ChampionStatsReadModel(tier, positionStats);
}
```

`getChampionStatsByPosition`은 **변경 없음**.

---

## Step 4: 어댑터 구현

파일: `module/infra/persistence/clickhouse/src/main/java/com/example/lolserver/repository/championstats/adapter/ChampionStatsClickHouseAdapter.java`

### 기존 메서드 수정
- `getChampionWinRates` → `getChampionWinRate`: position WHERE 조건 추가, 단일 객체 반환
- `getChampionMatchups`: position WHERE 조건 추가, List 반환 (Map 그룹화 제거)

### 기존 메서드 교체 (새 `*_agg` 테이블 사용)
- `getChampionRuneBuilds`: `rune_build_stats_local` → `champion_rune_stats_agg` (CTE 패턴, 개별 perk 컬럼)
- `getChampionSkillBuilds`: `skill_build_stats_local` → `champion_skill_build_stats_agg` (CTE 패턴)
- `getChampionItemBuilds`: `item_build_stats_local` → `champion_item_build_stats_agg` (CTE 패턴)

### 새 메서드 추가 (3개)
- `getChampionSpellStats`: `champion_spell_stats_agg` (Section 3 CTE 쿼리)
- `getChampionStartItemBuilds`: `champion_start_item_stats_agg` (Section 5 CTE 쿼리)
- `getChampionItemStats`: `champion_item_stats_agg` + `legendary_items` JOIN (Section 7 CTE 쿼리)

### 유지
- `getChampionStatsByPosition` - 변경 없음

---

## Step 5: 컨트롤러 변경

파일: `module/infra/api/src/main/java/com/example/lolserver/controller/championstats/ChampionStatsController.java`

`getChampionStats` 엔드포인트에 `@RequestParam("position") String position` 추가.

---

## Step 6: 테스트 업데이트

| 파일 | 변경 |
|---|---|
| `ChampionStatsServiceTest.java` | position 파라미터 추가, 새 포트 메서드 mock, 응답 구조 검증 |
| `ChampionStatsClickHouseAdapterTest.java` | 기존 메서드 시그니처 업데이트 + 새 메서드 3개 테스트 추가 |
| `ChampionStatsControllerTest.java` | position 파라미터 + 새 응답 필드 문서화 |

---

## 검증

```bash
./gradlew build
```

---

## 파일 변경 요약

| 액션 | 파일 |
|---|---|
| 교체 | `ChampionRuneBuildReadModel.java` |
| 교체 | `ChampionSkillBuildReadModel.java` |
| 교체 | `ChampionItemBuildReadModel.java` |
| 교체 | `ChampionPositionStatsReadModel.java` |
| 교체 | `ChampionStatsReadModel.java` |
| 추가 | `ChampionSpellStatsReadModel.java` |
| 추가 | `ChampionStartItemBuildReadModel.java` |
| 추가 | `ChampionItemStatsReadModel.java` |
| 교체 | `ChampionStatsQueryPort.java` |
| 교체 | `ChampionStatsService.java` |
| 교체 | `ChampionStatsClickHouseAdapter.java` |
| 수정 | `ChampionStatsController.java` |
| 교체 | `ChampionStatsServiceTest.java` |
| 교체 | `ChampionStatsClickHouseAdapterTest.java` |
| 교체 | `ChampionStatsControllerTest.java` |
