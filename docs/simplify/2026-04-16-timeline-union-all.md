# Simplify Review: Timeline UNION ALL Query Refactor

**Date:** 2026-04-16
**Target:** item_event + skill_level_up_event 2회 조회를 UNION ALL 네이티브 쿼리 1회로 통합

---

## 1. Code Reuse Review

### Findings

| # | Item | Action |
|---|------|--------|
| 1 | `Object[]` 네이티브 쿼리 매핑 — 기존 유틸리티 없음, 코드베이스 최초 사용 | **Skip** — 단일 사용처이므로 범용 유틸리티 불필요 |
| 2 | `buildTimelineData()` vs `TimelineData` 생성자 — 역할 분리 적절 (필터링 vs 집계) | **Skip** — 중복 아님 |
| 3 | 기존 EventSource 관련 enum/상수 없음 | **Fixed** — `TimelineEventDTO`에 `SOURCE_ITEM`, `SOURCE_SKILL` 상수 추가 |

---

## 2. Code Quality Review

### Findings

| # | Item | Severity | Action |
|---|------|----------|--------|
| 1 | "ITEM"/"SKILL" 매직 스트링이 SQL과 Java 필터에 산재 | High | **Fixed** — `TimelineEventDTO.SOURCE_ITEM/SOURCE_SKILL` 상수로 추출, Java 필터에 적용. SQL 리터럴은 가독성을 위해 유지 |
| 2 | `buildTimelineData()`가 리스트를 2회 순회 | Low | **Fixed** — 단일 패스 for 루프로 변경 |
| 3 | MatchMapper의 `// TimelineEventDTO -> Domain 변환` 불필요 주석 | Low | **Fixed** — 메서드명이 자명하므로 삭제 |
| 4 | `Object[]` 인덱스 기반 매핑이 SQL 컬럼 순서 변경에 취약 | Medium | **Skip** — SQL과 매퍼가 같은 클래스에 있어 동기화 위험 낮음, 테스트로 검증 |
| 5 | MatchMapper에 미사용 Entity 매핑 메서드 잔존 (`toDomain(ItemEventsEntity)` 등) | Low | **Skip** — 기존 테스트 유지, 별도 정리 태스크로 분리 |

---

## 3. Efficiency Review

### Findings

| # | Item | Impact | Action |
|---|------|--------|--------|
| 1 | `buildTimelineData` 2-pass → 1-pass 리팩토링 | Negligible | **Fixed** — 단일 for 루프로 변경 (200건 기준 미미하지만 코드 명확성 향상) |
| 2 | match_id 인덱스 존재 확인 — `(match_id, event_index)` 복합 유니크 인덱스 양 테이블에 존재 | N/A | **Skip** — 인덱스 최적화 충분 |
| 3 | UNION ALL 단일 쿼리 vs 2개 병렬 쿼리 — 단일 쿼리가 약간 더 효율적 (DB 커넥션/스레드 절감) | Low | **Skip** — 현재 구현이 적절 |
| 4 | `eventSource` 필드 메모리 오버헤드 — 200건 기준 ~1-2KB | Negligible | **Skip** — 무시 가능 수준 |
| 5 | CompletableFuture 3개 → 2개로 감소 (getMatchesBatch) | Low | **Skip** — vThread 풀이라 실질적 차이 미미 |

---

## Summary

| File | Change |
|------|--------|
| `repository/match/dto/TimelineEventDTO.java` | `SOURCE_ITEM`, `SOURCE_SKILL` 상수 추가 |
| `repository/match/adapter/MatchPersistenceAdapter.java` | `buildTimelineData()` 단일 패스 for 루프 + 상수 사용 |
| `repository/match/mapper/MatchMapper.java` | 불필요 주석 삭제 |

**Build result:** BUILD SUCCESSFUL
