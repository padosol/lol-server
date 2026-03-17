# 포지션별 챔피언 승률/픽률/밴률 조회 API로 교체

## Context

기존 `GET /api/v1/{platformId}/champion-stats/positions` API는 `champion_stats_local` 테이블에서 단순히 `(championId, totalGames)`만 반환했다. ClickHouse 스키마가 `champion_stats_agg`, `match_count_agg`, `champion_bans_agg` 3개의 SummingMergeTree 집계 테이블로 개편되면서, 이 API를 **승률(winRate), 픽률(pickRate), 밴률(banRate)** 를 반환하도록 교체한다. API URL과 파라미터는 동일하게 유지한다.

---

## 변경 순서

### 1. 새 ReadModel 생성

**`ChampionRateReadModel.java`** (신규)
- 위치: `module/core/lol-server-domain/.../championstats/application/model/`
```java
public record ChampionRateReadModel(int championId, double winRate, double pickRate, double banRate) {}
```

**`PositionChampionStatsReadModel.java`** (신규)
- 위치: 동일 패키지
```java
public record PositionChampionStatsReadModel(String teamPosition, List<ChampionRateReadModel> champions) {}
```

### 2. Port 인터페이스 변경
- 파일: `ChampionStatsQueryPort.java`
- `getChampionTotalGamesByPosition()` → `getChampionStatsByPosition()` 로 교체
- 반환 타입: `Map<String, List<ChampionRateReadModel>>`

### 3. Adapter SQL 교체
- 파일: `ChampionStatsClickHouseAdapter.java`
- `getChampionTotalGamesByPosition()` → `getChampionStatsByPosition()`
- `docs/05_champion_stats_query.sql` 의 CTE 쿼리 적용 (champion_stats_agg, match_count_agg, champion_bans_agg 사용)
- `nullIf/coalesce/round` 로 division-by-zero 방어 (기존 어댑터 패턴 준수)

### 4. Service 변경
- 파일: `ChampionStatsService.java`
- `getChampionTotalGamesByPosition()` → `getChampionStatsByPosition()`
- 반환: `List<PositionChampionStatsReadModel>`

### 5. Controller 변경
- 파일: `ChampionStatsController.java`
- `getChampionTotalGamesByPosition()` → `getChampionStatsByPosition()`
- `@GetMapping("/positions")` 경로 유지

### 6. 테스트 업데이트
- `ChampionStatsServiceTest.java` - 2개 테스트 메서드 교체
- `ChampionStatsClickHouseAdapterTest.java` - 1개 테스트 메서드 교체
- `ChampionStatsControllerTest.java` - 1개 테스트 + RestDocs 응답 필드 교체 (winRate, pickRate, banRate)

### 7. API 문서 업데이트
- `champion-stats-positions.adoc` - 제목 변경: "포지션별 챔피언 게임수 조회" → "포지션별 챔피언 승률/픽률/밴률 조회"

### 8. 구 모델 삭제
- `ChampionTotalGamesReadModel.java` 삭제
- `PositionChampionGamesReadModel.java` 삭제

---

## 주요 파일

| 파일 | 작업 |
|------|------|
| `.../application/model/ChampionRateReadModel.java` | 신규 생성 |
| `.../application/model/PositionChampionStatsReadModel.java` | 신규 생성 |
| `.../application/port/out/ChampionStatsQueryPort.java` | 메서드 교체 |
| `.../adapter/ChampionStatsClickHouseAdapter.java` | SQL 및 메서드 교체 |
| `.../application/ChampionStatsService.java` | 메서드 교체 |
| `.../controller/championstats/ChampionStatsController.java` | 메서드 교체 |
| `.../application/model/ChampionTotalGamesReadModel.java` | 삭제 |
| `.../application/model/PositionChampionGamesReadModel.java` | 삭제 |
| 테스트 3개 + adoc 1개 | 업데이트 |

---

## 검증

1. `./gradlew build` - 컴파일 및 전체 테스트 통과 확인
2. RestDocs 테스트로 API 응답 필드 문서 자동 생성 확인
