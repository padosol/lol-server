# startItems / itemBuild 응답을 number[] 배열로 변경

## Context

**문제**: `GET /api/v1/{platformId}/champion-stats` 응답에서 다음 두 필드가 JSON-stringified 배열로 직렬화돼 내려감.

- `data.positions[*].startItemBuilds[*].startItems` → 예: `"[1101,2003]"` (string)
- `data.positions[*].itemBuilds[*].itemBuild` → 예: `"[3161,6699,6694]"` (string)

원인: BigQuery 어댑터가 `start_item_ids_json`(BQ 원본 JSON 문자열 컬럼)과 `TO_JSON_STRING([item1,item2,item3])`(BQ에서 JSON 직렬화한 결과)을 그대로 `getStringValue()`로 받아 모델 `String` 필드에 담음.

**프론트 영향**: `lol-ui/src/widgets/champion-stats-panel/ui/ItemBuildStats.tsx:26-29, 49-52`에서 `.split(",").map(Number).filter(Boolean)` 로 파싱 중. 이 로직은 CSV(`1101,2003`) 형식만 지원하고 JSON 배열 형식(`[1101,2003]`)은 처리 못 함 → `Number("[1101")`/`Number("2003]")`가 모두 `NaN` → `filter(Boolean)`이 NaN 제거 → **빈 배열 → 아이템 미렌더**. 이게 PR #148 itemBuild 미수신의 진짜 원인.

**해결**: 백엔드 응답에서 두 필드를 처음부터 `List<Integer>` 배열로 직렬화. FE 파싱 로직 제거.

**범위**: startItems, itemBuild 두 필드만. skillBuild는 BQ 응답에 `null` 포함(`[3,1,...,null,null]`)되고 ClickHouse legacy에선 `Q,E,W,...` 문자라 타입 일관성을 깨므로 이번 변경에서 제외.

---

## 수정 파일 목록

### Domain models (필드 타입 변경)

- `module/core/lol-server-domain/src/main/java/com/example/lolserver/domain/championstats/application/model/ChampionStartItemBuildReadModel.java`
  - `String startItems` → `List<Integer> startItems`
- `module/core/lol-server-domain/src/main/java/com/example/lolserver/domain/championstats/application/model/ChampionItemBuildReadModel.java`
  - `String itemBuild` → `List<Integer> itemBuild`

### BigQuery 어댑터 (JSON 파싱)

- `module/infra/persistence/bigquery/src/main/java/com/example/lolserver/repository/championstats/adapter/ChampionStatsBigQueryAdapter.java`
  - 클래스 상단에 `private static final ObjectMapper MAPPER = new ObjectMapper();` 추가
  - 헬퍼 추가:
    ```java
    private static List<Integer> parseIntArrayJson(String json) {
        try {
            return MAPPER.readValue(json, new TypeReference<List<Integer>>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to parse int array: " + json, e);
        }
    }
    ```
  - `getChampionStartItemBuilds` 매핑: `row.get("start_items").getStringValue()` → `parseIntArrayJson(row.get("start_items").getStringValue())`
  - `getChampionItemBuilds` 매핑: `row.get("item_build").getStringValue()` → `parseIntArrayJson(row.get("item_build").getStringValue())`
  - SQL은 그대로 둠 (BQ 원본 JSON 문자열 컬럼 + `TO_JSON_STRING(...)` 그대로). Java 측에서만 파싱.

### ClickHouse 어댑터 (CSV 파싱)

- `module/infra/persistence/clickhouse/src/main/java/com/example/lolserver/repository/championstats/adapter/ChampionStatsClickHouseAdapter.java`
  - 헬퍼 추가:
    ```java
    private static List<Integer> parseCsvIntArray(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Integer::parseInt)
                .toList();
    }
    ```
  - `getChampionStartItemBuilds` 매핑: `rs.getString("start_items")` → `parseCsvIntArray(rs.getString("start_items"))`
  - `getChampionItemBuilds` 매핑: `rs.getString("item_build")` → `parseCsvIntArray(rs.getString("item_build"))`

### 캐시 prefix bump v3 → v4

응답 모델 shape 변경(`String` → `List<Integer>`)으로 v3 캐시 stale 역직렬화 fail 가능 → 새 prefix.

- `module/infra/persistence/redis/src/main/java/com/example/lolserver/repository/championstats/ChampionStatsCacheAdapter.java`
  - `champion-stats:v3:detail:` → `champion-stats:v4:detail:`
  - `champion-stats:v3:positions:` → `champion-stats:v4:positions:`

### 테스트 업데이트

- `module/core/lol-server-domain/src/test/java/com/example/lolserver/domain/championstats/application/ChampionStatsServiceTest.java`
  - mock 데이터 변경:
    - `new ChampionStartItemBuildReadModel("1056,2003", ...)` → `new ChampionStartItemBuildReadModel(List.of(1056, 2003), ...)`
    - `new ChampionItemBuildReadModel("3089,3157,3165", ...)` → `new ChampionItemBuildReadModel(List.of(3089, 3157, 3165), ...)`
- `module/infra/persistence/clickhouse/src/test/java/com/example/lolserver/repository/championstats/adapter/ChampionStatsClickHouseAdapterTest.java`
  - 동일하게 `String` → `List.of(...)`. assertion도 `.startItems()` / `.itemBuild()` 비교 수정.
- `module/infra/api/src/test/java/com/example/lolserver/docs/controller/ChampionStatsControllerTest.java`
  - mock: `new ChampionStartItemBuildReadModel("1054,2003", ...)` → `List.of(1054, 2003)`
  - mock: `new ChampionItemBuildReadModel("3078,3053,3065", ...)` → `List.of(3078, 3053, 3065)`
  - RestDocs `responseFields` 필드 타입 변경:
    - `data.positions[].startItemBuilds[].startItems` `JsonFieldType.STRING` → `JsonFieldType.ARRAY`
    - 해당 배열 원소 타입 표기 (`startItems[]` 추가 필드로 NUMBER 등)
    - `data.positions[].itemBuilds[].itemBuild` 동일 처리
- `module/infra/persistence/redis/src/test/java/com/example/lolserver/repository/championstats/ChampionStatsCacheAdapterTest.java`
  - `expectedKey` v3 → v4 일괄 교체 (8군데)
- `module/infra/persistence/bigquery/src/test/java/com/example/lolserver/repository/championstats/adapter/ChampionStatsBigQueryAdapterTest.java`
  - 현재 startItem/item 어댑터 메서드 단위 테스트는 없음. 굳이 새로 추가하지 않아도 무방하나, JSON 파싱 헬퍼 동작 검증 위해 1건 추가 권장 (옵션):
    - `getChampionItemBuildsParsesJsonArray` — `"[3078,3053,3065]"` → `List.of(3078, 3053, 3065)` 매핑 확인.

### RestDocs 문서 description

- 두 필드 description 업데이트:
  - `startItems`: "시작 아이템 ID 목록" (현재 동일하지만 type ARRAY 명시)
  - `itemBuild`: "코어 아이템 ID 순서" (현재 동일하지만 type ARRAY 명시)

---

## FE (lol-ui) 변경 — pane2 지시

별도 브랜치에서 처리. 두 변경은 백엔드 머지 후 FE 머지(또는 동시 머지) 필요.

### types.ts (`lol-ui/src/entities/champion/types.ts`)

```diff
 export interface ItemBuildData {
-  itemBuild: string; // "3078,3053,3065"
+  itemBuild: number[]; // [3078, 3053, 3065]
   games: number;
   winRate: number;
   pickRate: number;
 }

 export interface StartItemBuildData {
-  startItems: string; // "1054,2003"
+  startItems: number[]; // [1054, 2003]
   games: number;
   winRate: number;
   pickRate: number;
 }
```

### ItemBuildStats.tsx (`lol-ui/src/widgets/champion-stats-panel/ui/ItemBuildStats.tsx`)

`.split(",").map(Number).filter(Boolean)` 두 군데(L26-29, L49-52) 제거하고 배열 그대로 사용:

```diff
 {startItemBuilds.map((build, i) => {
-  const itemIds = build.startItems
-    .split(",")
-    .map(Number)
-    .filter(Boolean);
   return (
     <BuildRow
       key={i}
-      itemIds={itemIds}
+      itemIds={build.startItems}
       ...
```

```diff
 {data.map((build, i) => {
-  const itemIds = build.itemBuild
-    .split(",")
-    .map(Number)
-    .filter(Boolean);
   return (
     <BuildRow
       key={i}
-      itemIds={itemIds}
+      itemIds={build.itemBuild}
       ...
```

### skillBuild는 그대로 유지

`SkillBuildData.skillBuild: string` 그대로. 본 변경 범위 아님 (BQ null 처리 + ClickHouse legacy 호환 이슈로 별도 추후 검토).

### FE 검증 케이스

```
GET http://localhost:8100/api/v1/kr/champion-stats?championId=266&patch=16.5&tier=EMERALD%2B
```

→ `positions[0]` (TOP) 의 `startItemBuilds[].startItems`, `itemBuilds[].itemBuild` 둘 다 `number[]` 형태로 내려와 아이템 이미지 정상 렌더 확인.

---

## 검증 절차 (백엔드)

1. **컴파일**: `./gradlew compileJava compileTestJava`
2. **단위 테스트**: `./gradlew test`
3. **RestDocs 빌드**: `./gradlew :module:infra:api:asciidoctor`
4. **로컬 라이브 검증** (local 프로파일 부팅 상태에서):
   ```bash
   curl -s 'http://localhost:8100/api/v1/kr/champion-stats?championId=266&patch=16.5&tier=EMERALD%2B' \
     | jq '.data.positions[0] | {startItem: .startItemBuilds[0].startItems, item: .itemBuilds[0].itemBuild}'
   ```
   기대 출력: `{startItem: [1054, 2003], item: [3161, 6699, 6694]}` (배열, 따옴표 없음)
5. **캐시 검증**: `docker exec lol-redis redis-cli KEYS "champion-stats:v4:*"` → 새 호출 후 v4 키 생성 확인.

---

## 커밋

`feature/MP-8-bigquery-stats-module` 브랜치 그대로 진행 (현재 작업 연장선).

```
refactor: MP-8 startItems/itemBuild 응답을 number[] 배열로 변경

ChampionStartItemBuildReadModel.startItems / ChampionItemBuildReadModel.itemBuild
필드를 String → List<Integer>로 전환. BQ 어댑터는 Jackson으로 JSON 문자열 파싱,
ClickHouse 어댑터는 CSV split. 응답 shape 변경에 따라 Redis 캐시 prefix v3→v4 bump.

Created-By: Padosol
```

---

## 영향 받지 않는 부분

- `bootBuilds[].bootId` — 단일 ID, 영향 없음
- `runeBuilds[].*` — 모두 개별 number 필드, 영향 없음
- `spellStats[].summoner1Id`/`summoner2Id` — 영향 없음
- `skillBuilds[].skillBuild` — 본 변경에서 제외 (별도 이슈)
- `matchups[].*` — 영향 없음
- 포지션별 챔피언 통계(`/positions` 엔드포인트) — 영향 없음
- 캐시 어댑터 일반 로직 — prefix만 변경
