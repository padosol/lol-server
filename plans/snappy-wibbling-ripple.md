# 게임 시작 전 선택 기반 통계 (시작 아이템, 아이템 빌드, 스킬 트리)

## Context

매치에서 **게임 시작 전 결정되는 요소** (챔피언, 라인, 룬, 소환사 주문)를 기반으로
**게임 중 결정되는 요소** (시작 아이템, 아이템 빌드, 스킬 트리)의 통계를 내려는 요구사항.

## 현재 스키마 검토 결과

### 팩트 테이블 (`match_participant_local`) - ✅ 데이터 충분

| 구분 | 필드 | 상태 |
|------|------|------|
| 챔피언 | `champion_id` | ✅ |
| 라인 | `team_position` | ✅ |
| 룬 | `primary_style_id`, `primary_perk_ids`, `sub_style_id`, `sub_perk_ids` | ✅ |
| 소환사 주문 | `summoner1_id`, `summoner2_id` | ✅ |
| 최종 아이템 | `item0`~`item6` | ✅ |
| 아이템 빌드 순서 | `item_build_order` (Array) | ✅ |
| 스킬 순서 | `skill_order` (Array) | ✅ |

**결론: 팩트 테이블에 필요한 모든 데이터가 존재합니다.**

### 현재 집계 테이블 (Materialized View) - ⚠️ 그룹핑 키 불일치

현재 MV들의 그룹핑 키: `(champion_id, team_position, tier, patch, platform_id)`

| 통계 | 테이블 | 룬/소환사주문 포함 여부 |
|------|--------|----------------------|
| 승률 | `champion_stats_local` | ❌ |
| 아이템 빌드 | `item_build_stats_local` | ❌ |
| 스킬 빌드 | `skill_build_stats_local` | ❌ |
| 룬 빌드 | `rune_build_stats_local` | ❌ (룬 자체가 집계 대상) |
| 매치업 | `champion_matchup_stats_local` | ❌ |

**현재 MV로는 "(챔피언+라인+룬+소환사주문) → 아이템/스킬" 조합 통계를 낼 수 없음**

## 구현 가능성 분석

### 방안 1: 팩트 테이블 직접 쿼리 (권장)

`match_participant_local`에 WHERE 필터로 직접 조회:

```sql
-- 예: 야스오(157) + 미드(MIDDLE) + 정복자(8010) + 점멸(4)+점화(14) 조합의
-- 시작 아이템 / 아이템 빌드 / 스킬 트리 통계

-- 시작 아이템 (첫 N개 아이템)
SELECT
    arraySlice(item_build_order, 1, 3) AS starting_items,
    count() AS games,
    sum(win) AS wins
FROM match_participant_local FINAL
WHERE champion_id = 157
  AND team_position = 'MIDDLE'
  AND primary_perk_ids[1] = 8010  -- 정복자 (키스톤)
  AND summoner1_id = 4 AND summoner2_id = 14
  AND patch = '14.24'
GROUP BY starting_items
ORDER BY games DESC
LIMIT 5;
```

**장점**: 추가 테이블/MV 불필요, 유연한 필터링
**단점**: 매번 팩트 테이블 스캔 (ClickHouse 특성상 충분히 빠름)

### 방안 2: 새 MV 추가

룬+소환사주문을 그룹핑 키에 포함하는 새 MV 생성.

**문제점**: 카디널리티 폭발
- 챔피언(~170) × 포지션(5) × 룬 조합(수백) × 소환사 주문 조합(~20) × 티어(~8) × 패치
- 수천만 행 이상으로 사전 집계 비용이 큼

### 결론

| 항목 | 가능 여부 | 비고 |
|------|----------|------|
| 시작 아이템 통계 | ✅ 가능 | `arraySlice(item_build_order, 1, N)`으로 추출 |
| 아이템 빌드 통계 | ✅ 가능 | 최종 아이템 `arraySort([item0..item5])` 또는 `item_build_order` 활용 |
| 스킬 트리 통계 | ✅ 가능 | `arraySlice(skill_order, 1, 15)` 활용 |

**팩트 테이블 직접 쿼리(방안 1)가 가장 적합합니다.**

### 시작 아이템 관련 한계점

`item_build_order`에는 타임스탬프가 없어 "몇 분에 구매했는지" 구분이 안 됨.
- 현재: 구매 순서만 알 수 있음 (첫 N개 = 시작 아이템으로 간주)
- 시작 아이템과 첫 귀환 아이템을 정확히 구분하려면 `item_build_order`에 타임스탬프를 추가하거나, PostgreSQL의 `item_events` 테이블에서 직접 조회해야 함

## 구현 계획

### 1단계: `ChampionStatsQueryPort`에 새 쿼리 메서드 추가

```java
// 특정 룬+소환사주문 조합의 시작 아이템 통계
List<StartingItemBuildReadModel> getStartingItemBuilds(
    int championId, String patch, String platformId,
    String tier, String teamPosition,
    int primaryPerkKeystone, int summoner1Id, int summoner2Id);

// 특정 룬+소환사주문 조합의 아이템 빌드 통계
List<ChampionItemBuildReadModel> getFilteredItemBuilds(
    int championId, String patch, String platformId,
    String tier, String teamPosition,
    int primaryPerkKeystone, int summoner1Id, int summoner2Id);

// 특정 룬+소환사주문 조합의 스킬 트리 통계
List<ChampionSkillBuildReadModel> getFilteredSkillBuilds(
    int championId, String patch, String platformId,
    String tier, String teamPosition,
    int primaryPerkKeystone, int summoner1Id, int summoner2Id);
```

### 2단계: ReadModel 추가
- `StartingItemBuildReadModel`: `startingItems` (List<Integer>), `totalGames`, `totalWins`, `totalWinRate`

### 3단계: ClickHouse 어댑터 구현
- `ChampionStatsClickHouseAdapter`에 팩트 테이블 직접 쿼리 메서드 추가

### 4단계: API 엔드포인트 추가/확장
- 기존 `/api/v1/{platformId}/champion-stats`에 룬/소환사주문 필터 파라미터 추가
- 또는 별도 엔드포인트 생성

### 수정 대상 파일
- `ChampionStatsQueryPort.java` (포트)
- `ChampionStatsService.java` (서비스)
- `ChampionStatsClickHouseAdapter.java` (어댑터)
- `ChampionStatsController.java` (컨트롤러)
- 새 ReadModel 추가 (필요 시)

### 검증
- ClickHouse에 테스트 쿼리 직접 실행하여 결과 확인
- 어댑터 단위 테스트
- API 통합 테스트
