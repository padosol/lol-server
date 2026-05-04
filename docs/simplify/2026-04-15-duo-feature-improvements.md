# Simplify Review: Duo Feature Improvements

**Date:** 2026-04-15
**Target:** Duo finding feature - desiredLane rename, expiration change, mostChampions/recentGameSummary addition

---

## 1. Code Reuse Review

### Findings

| # | Item | Action |
|---|------|--------|
| 1 | Two JSON converters (`MostChampionListConverter`, `RecentGameSummaryConverter`) share ~90% identical boilerplate (null checks, ObjectMapper, exception wrapping). Could extract an `AbstractJsonAttributeConverter<T>` base class. | **Skip** -- only 2 converters exist currently; extract when a 3rd is added |
| 2 | `RiotAccountResolver` calls `MatchPersistencePort` (output port) directly instead of `MatchQueryUseCase` (input port), violating hexagonal architecture port direction. | **Skip** -- valid concern but would introduce cross-context coupling via input ports; current approach keeps the dependency explicit through infra-layer wiring |
| 3 | `lookupRecentGameSummary()` duplicates pagination constants already in `MatchService` (page size 20, sort field "match", DESC). | **Skip** -- the duo-specific constants serve a different purpose (snapshot size) even if values happen to match |
| 4 | `new java.util.ArrayList<>()` used without import in `RiotAccountResolver` | **Fixed** -- added proper `import java.util.ArrayList` |
| 5 | Identical test fixture helpers (`createTestDuoPost`, `createTestDuoRequest`) duplicated in `DuoServiceTest` and `DuoRequestServiceTest` | **Skip** -- extracting shared test fixtures is worthwhile but out of scope for this review; low maintenance cost with only 2 test files |

---

## 2. Code Quality Review

### Findings

| # | Item | Severity | Action |
|---|------|----------|--------|
| 1 | FQ `new java.util.ArrayList<>()` in `RiotAccountResolver` | Low | **Fixed** -- proper import added |
| 2 | `DuoPost.create()` has 9 params, `DuoRequest.create()` has 10 -- parameter sprawl | Medium | **Skip** -- introducing a parameter object adds indirection; the factory method is called from only 1 place each |
| 3 | `DuoPost.updateContent()` does not refresh `mostChampions`/`recentGameSummary` | Medium | **Skip** -- intentional behavior: stats are a snapshot at creation time; update only changes user-editable fields |
| 4 | `enrichWithJsonFields` second query loads full entities for 2 columns | Medium | **Skip** -- batch query on page-size rows (~20), overhead is minimal vs. complexity of native query workaround |
| 5 | `DuoPostListDTO` mixes `@QueryProjection` constructor with mutable setters | Medium | **Skip** -- pragmatic workaround for QueryDSL JSON column limitation; lifecycle is contained in `DuoPostRepositoryCustomImpl` |
| 6 | Static `ObjectMapper` in converters is disconnected from Spring Jackson config | Low | **Skip** -- only serializes simple records without dates; no risk of divergence |
| 7 | Converter error handling throws on deserialization failure | Low | **Skip** -- consistent with JPA converter contract; corrupted JSON should fail loudly |
| 8 | Stale `plusHours(24)` in `DuoServiceTest` matched-post test (production uses 1h) | Medium | **Fixed** -- changed to `plusHours(1)` |
| 9 | FQ `org.mockito.ArgumentMatchers` in `RiotAccountResolverTest` | Low | **Fixed** -- replaced with static imports |
| 10 | Duplicated test fixture helpers across test classes | Low | **Skip** -- same as Reuse #5 |

---

## 3. Efficiency Review

### Findings

| # | Item | Impact | Action |
|---|------|--------|--------|
| 1 | `lookupRecentGameSummary` loads full match data (50+ fields x 10 participants x 20 games) to extract 3 fields | Medium | **Skip** -- requires new port method with lightweight projection; separate optimization task |
| 2 | Three sequential lookups (tierInfo, mostChampions, recentGameSummary) in `createDuoPost` (~3x latency) | High | **Fixed** -- added `lookupAllStats()` with `CompletableFuture` parallel execution |
| 3 | Same sequential lookup issue in `createDuoRequest` | High | **Fixed** -- both services now use `lookupAllStats()` |
| 4 | `enrichWithJsonFields` second batch query for JSON columns | Low | **Skip** -- single `IN` query on ~20 IDs, not N+1; acceptable overhead |
| 5 | Static ObjectMapper instances in converters | Negligible | **Skip** -- correct and thread-safe |

---

## Summary

| File | Change |
|------|--------|
| `RiotAccountResolver.java` | Fixed FQ `ArrayList` import; added `lookupAllStats()` with parallel `CompletableFuture` execution; added `RiotAccountStats` record |
| `DuoService.java` | Replaced 3 sequential lookups with single `lookupAllStats()` call |
| `DuoRequestService.java` | Replaced 3 sequential lookups with single `lookupAllStats()` call |
| `DuoServiceTest.java` | Fixed stale `plusHours(24)` → `plusHours(1)`; updated mocks to use `lookupAllStats` |
| `DuoRequestServiceTest.java` | Updated mocks to use `lookupAllStats` |
| `RiotAccountResolverTest.java` | Fixed FQ `org.mockito.ArgumentMatchers` → static imports |

**Build result:** BUILD SUCCESSFUL
