# Code Review: 챔피언 통계 TierFilter 도입 + 티어 계산기 리팩토링

- **날짜**: 2026-03-17
- **브랜치**: `refactor/ecr-deploy-pipeline`
- **변경 파일**: 13개 (1,752 additions, 1,805 deletions)

---

## 변경 개요

이 변경은 크게 **3가지**로 구성됩니다:

1. **`TierFilter` 값 객체 도입** — 단일 티어(`EMERALD`)와 범위 티어(`MASTER+`)를 지원하는 필터를 추가하고, 전 계층(`Controller → Service → Port → Adapter`)에서 `String tier` → `TierFilter tierFilter`로 전환
2. **`ChampionTierCalculator` 알고리즘 대폭 단순화** — robust z-score + 백분위 fallback + tier cap 등 복잡한 로직을 제거하고, 시그모이드/지수 함수 기반의 절대 점수(0~100) 체계로 교체
3. **기타** — 성능 테스트 데이터 갱신, k6 테스트 데이터 소스 변경, QueryDSL 들여쓰기 정리

---

## 긍정적인 부분

- **`TierFilter` 설계가 깔끔합니다.** 불변 객체, `of()` 팩토리 메서드, `Tier` enum의 `score`를 활용한 범위 계산이 잘 구성됨
- **테스트 커버리지가 우수합니다.** `TierFilterTest`에서 단일/범위/경계값/에러 케이스를 모두 커버. `ChampionTierCalculatorTest`에도 새 알고리즘에 맞는 테스트가 추가됨
- **ClickHouse 어댑터에서 `tier IN (...)` 절로의 전환이 일관적입니다.** `tierInClause()` 유틸 메서드를 통해 모든 쿼리를 통일
- **티어 계산기 단순화로 코드량이 크게 감소**했고 (약 -100줄), 이해하기 훨씬 쉬워짐

---

## 이슈 및 제안

### [Critical] SQL Injection 위험 — `tierInClause()`

`ChampionStatsClickHouseAdapter.java`의 `tierInClause()`:

```java
private static String tierInClause(TierFilter tierFilter) {
    return tierFilter.getTierNames().stream()
            .map(ChampionStatsClickHouseAdapter::quote)
            .collect(Collectors.joining(", ", "tier IN (", ")"));
}
```

현재 `TierFilter.of()`가 `Tier.valueOf()`로 검증하므로 실질적으로 안전하지만, `quote()` 메서드를 통한 문자열 삽입(`String.formatted`)은 구조적으로 취약합니다. **ClickHouse JDBC의 `PreparedStatement`를 사용할 수 없는 것인지 확인 필요**합니다. 만약 PreparedStatement가 불가하다면, 최소한 `tierInClause()` 메서드에 방어적 주석을 추가하는 것을 권장합니다.

### [Medium] `TierFilter`에 `equals()`/`hashCode()` 미구현

`TierFilter`가 테스트에서 Mockito `given(...).willReturn(...)` 매칭에 사용되는데, `equals()`/`hashCode()`가 없으면 `TierFilter.of("EMERALD")` 두 번 호출 시 서로 다른 객체로 인식됩니다.

현재 `ChampionStatsServiceTest`에서는 같은 `tierFilter` 인스턴스를 stub과 호출 모두에 사용하므로 문제없지만, `ChampionStatsControllerTest`에서는 `any(TierFilter.class)`로 우회하고 있습니다. **`equals()`/`hashCode()`를 구현하거나 record로 전환**하면 더 정밀한 매칭이 가능합니다.

### [Medium] Controller의 공백→`+` 변환 로직이 의아함

```java
private TierFilter parseTierFilter(String tier) {
    String normalized = tier.endsWith(" ") ? tier.stripTrailing() + "+" : tier;
    return TierFilter.of(normalized);
}
```

`"MASTER "` → `"MASTER+"`로 변환하는 이유가 불명확합니다. URL에서 `+`가 공백으로 디코딩되는 것을 보상하기 위한 것이라면, **`@RequestParam`의 URL 디코딩 동작과 함께 명시적 주석**이 필요합니다. 또한 `"MASTER  "` (공백 2개)나 중간에 공백이 있는 케이스도 고려해야 합니다. `tier.strip()`으로 앞뒤 공백을 모두 제거한 후 처리하는 것이 더 안전합니다.

### [Low] `TierFilter`가 `core:enum` 모듈에 위치

`TierFilter`는 단순 enum이 아닌 비즈니스 로직(범위 계산)을 포함합니다. `core:enum` 모듈의 목적이 "공유 enum 타입"이라면, 이 클래스의 위치가 적절한지 검토할 필요가 있습니다. `Tier` enum에 의존하므로 같은 모듈에 두는 것이 합리적이긴 하지만, 모듈 이름과의 괴리가 있습니다.

### [Low] 티어 경계값 매직 넘버

`tierFromScore()`의 경계값들(`80.0`, `65.0`, `53.0`, `47.0`, `35.0`)과 시그모이드 파라미터(`-40.0`, `-30.0`, `-15.0`)가 상수로 추출되지 않았습니다. 향후 튜닝 시 가독성을 위해 상수화를 고려해볼 수 있습니다.

### [Low] `MatchSummonerRepositoryCustomImpl` 변경

QueryDSL 조인의 들여쓰기만 변경된 것으로 보이는데, 이 변경이 이 PR의 범위에 포함된 이유가 불분명합니다. 별도 커밋으로 분리하는 것이 이력 추적에 유리합니다.

---

## 테스트 관련

- `singleChampion_getsScoreBasedTier` 테스트에서 `assertThat(result.get(0).tier()).isIn("OP", "1", "2", "3", "4", "5")`는 모든 유효 티어를 허용하므로 사실상 아무것도 검증하지 못합니다. 해당 입력(winRate=0.52, pickRate=0.08, banRate=0.05, totalGames=1500)에 대한 **기대 티어를 계산하여 구체적으로 검증**하는 것이 좋습니다.
- `absoluteScore_isConsistentAcrossGroupSizes` 테스트는 새 알고리즘의 핵심 강점(그룹 크기 독립성)을 잘 검증합니다.

---

## 전체 평가

아키텍처적으로 잘 설계된 변경입니다. `String` → `TierFilter` 전환이 전 계층에 일관되게 적용되었고, 티어 계산기 단순화도 이전의 과도하게 복잡한 로직을 효과적으로 대체합니다. 위의 `equals()`/`hashCode()` 구현과 Controller의 공백 변환 로직 개선을 권장합니다.
