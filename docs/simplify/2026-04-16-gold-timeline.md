# Simplify Review: Gold Timeline Feature

**Date:** 2026-04-16
**Target:** participant_frame 골드 타임라인 조회 및 팀별 누적 골드 계산 추가

---

## 1. Code Reuse Review

### Findings

| # | Item | Action |
|---|------|--------|
| 1 | `toInt(Object)` helper in MatchRepositoryCustomImpl duplicates inline pattern in TimelineRepositoryCustomImpl | **Skip** — 2곳 사용, 공유 유틸 추출은 과도한 추상화 |
| 2 | `convertToGameData`에서 `matchTeamRepository.findByMatchId()` 별도 호출 — MatchSummonerDTO에 이미 LEFT JOIN으로 팀 데이터 포함 | **Fixed** — `buildTeamInfoData()` 재사용으로 통합, `matchTeamRepository` 의존성 제거 |
| 3 | assembleGameDataFromDTO와 convertToGameData에서 teamId 그룹핑 + TeamData 조립 패턴 중복 | **Fixed** — 양쪽 모두 `buildTeamInfoData(byTeam.get(100/200))` 패턴으로 통일 |

---

## 2. Code Quality Review

### Findings

| # | Item | Severity | Action |
|---|------|----------|--------|
| 1 | `@JsonProperty("timeccing_others")` — DTO는 Object[] 매핑으로 채워지므로 Jackson 어노테이션 무의미 | High | **Fixed** — 제거 |
| 2 | 불필요한 주석: `// 팀 누적 골드 타임라인`, `// 골드 타임라인 (participant_frame JOIN)` | Low | **Fixed** — 제거 |
| 3 | 60+ 컬럼 positional Object[] 매핑 (toMatchSummonerDTO) — 컬럼 순서 변경 시 사일런트 버그 가능 | Medium | **Skip** — native SQL 전환에 따른 트레이드오프. 통합 테스트로 커버 필요 |
| 4 | Magic numbers 100/200 for teamId | Low | **Skip** — 코드베이스 전반에 존재하는 기존 패턴 |
| 5 | `timestamps` 필드가 팀/참가자 레벨이 아닌 매치 레벨 데이터 | Low | **Skip** — 스키마 변경 필요, 별도 작업으로 추적 |

---

## 3. Efficiency Review

### Findings

| # | Item | Impact | Action |
|---|------|--------|--------|
| 1 | `convertToGameData`에서 `matchTeamRepository.findByMatchId()` 불필요 쿼리 — 이미 native SQL JOIN에 팀 데이터 포함 | Medium | **Fixed** — `buildTeamInfoData()` 재사용으로 쿼리 1회 제거 |
| 2 | `timestamps` 배열이 참가자 10명에 동일하게 중복 전송 | Low | **Skip** — ~400 int 수준, 스키마 변경 필요 |
| 3 | `Integer[]` vs `int[]` 박싱 오버헤드 | Negligible | **Skip** — 현재 규모에서 무시 가능 |
| 4 | `convertToGameData` N+1 패턴 (매치당 3쿼리) | High | **Skip** — 기존 이슈, 이번 변경과 무관. getMatchesBatch에서는 이미 배치 처리 |

---

## Summary

| File | Change |
|------|--------|
| `MatchSummonerDTO.java` | `@JsonProperty` 제거, 불필요 import/주석 제거 |
| `TeamInfoData.java` | 불필요 주석 제거 |
| `MatchPersistenceAdapter.java` | `convertToGameData` → `buildTeamInfoData` 재사용으로 통합, `matchTeamRepository` 의존성 제거 (필드/생성자/import) |

**Build result:** root 권한 build 캐시 문제로 빌드 검증 보류 — `sudo rm -rf module/infra/persistence/postgresql/build/tmp/compileJava/compileTransaction` 실행 후 확인 필요
