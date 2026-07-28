# Simplify Review: Duo Feature & Riot RSO PUUID

**Date:** 2026-04-13
**Target:** Riot RSO PUUID 연동 및 듀오 찾기 기능 기반 구현 (62 files, +3183/-29)

---

## 1. Code Reuse Review

### Findings

| # | Item | Action |
|---|------|--------|
| 1 | `CreateDuoPostCommand`와 `CreateDuoRequestCommand`가 동일한 4개 필드를 가진 중복 클래스 | **Skip** — 향후 요구사항 분화 가능성이 있어 의도적 분리로 판단 |
| 2 | `DuoService.extractRiotPuuid()` 로직이 서비스에 위치 — Member 도메인 객체에 `findRiotPuuid()` 메서드로 이동 가능 | **Skip** — 현재 유일한 사용처이므로 추후 재사용 시점에 리팩토링 |
| 3 | `DuoPostDetailReadModel`에 `of()` 팩토리 메서드 없이 서비스에서 인라인 빌더 사용 | **Fixed** — `DuoPostDetailReadModel.of(DuoPost, boolean, List)` 팩토리 메서드 추가 |
| 4 | `DuoPostRepositoryCustomImpl`의 `findActivePosts()`와 `findByMemberId()`가 QueryDSL 로직 거의 중복 | **Fixed** — `queryDuoPosts(Pageable, BooleanExpression...)` 공통 헬퍼 메서드로 추출 |
| 5 | 컨트롤러 간 `SliceResponse` 변환 패턴 불일치 (`toSlice()` vs 수동 변환) | **Skip** — 기능상 영향 없는 스타일 차이, 추후 일괄 통일 |
| 6 | 새 ReadModel들이 Java `record` 대신 `@Getter @Builder class` 사용 (CLAUDE.md 컨벤션 위반) | **Skip** — 기존 ReadModel에도 같은 패턴 존재하여 일괄 마이그레이션 필요 |
| 7 | `DuoMatchResultReadModel` 생성 시 하드코딩 상태 문자열 ("ACCEPTED", "CONFIRMED") | **Fixed** — `DuoRequestStatus.ACCEPTED.name()`, `DuoRequestStatus.CONFIRMED.name()` 사용 |

---

## 2. Code Quality Review

### Findings

| # | Item | Severity | Action |
|---|------|----------|--------|
| 1 | `DuoService.extractRiotPuuid()`에서 매직 스트링 `"RIOT"` 사용 — `OAuthProvider.RIOT.name()` 사용 필요 | Medium | **Fixed** — `OAuthProvider.RIOT.name()` 으로 교체 |
| 2 | `DuoService.lookupTierInfo()`에서 매직 스트링 `"RANKED_SOLO_5x5"` 사용 — `QueueType` enum 존재 | Medium | **Fixed** — `QueueType.RANKED_SOLO_5x5.name()` 으로 교체 |
| 3 | `DuoPostRepositoryCustomImpl`에서 `"ACTIVE"` 매직 스트링 사용 — `DuoPostStatus` enum 존재 | Low | **Fixed** — `DuoPostStatus.ACTIVE.name()` 으로 교체 |
| 4 | `DuoPostSearchCommand`에 불필요한 `@Setter` — 빌더 패턴만으로 충분 | Low | **Fixed** — `@Setter` 제거 |
| 5 | `DuoPostReadModel.tierAvailable`이 `tier != null`에서 파생 가능한 중복 상태 | Low | **Skip** — API 소비자 편의를 위한 의도적 필드 |
| 6 | `Member.linkSocialAccount()`에 6개 positional 파라미터 (parameter sprawl) | Medium | **Skip** — 기존 API 전반에 영향을 주는 큰 리팩토링, 별도 작업 필요 |
| 7 | `DuoRequestJpaRepository`의 JPQL 쿼리에 하드코딩된 상태 문자열 | Low | **Skip** — JPQL 쿼리 내 상수는 enum 참조 불가, 기존 패턴과 일치 |

---

## 3. Efficiency Review

### Findings

| # | Item | Impact | Action |
|---|------|--------|--------|
| 1 | `findByDuoPostId()`가 무제한 리스트 반환 — 인기 게시글에서 메모리 이슈 가능 | Medium | **Skip** — 페이지네이션 추가는 기능 변경, 별도 이슈로 추적 권장 |
| 2 | Riot PUUID 조회가 OIDC 로그인 핫패스에 블로킹 HTTP 콜 추가 | Medium | **Skip** — Riot OIDC의 sub claim과 PUUID가 다를 수 있어 검증 필요 |
| 3 | `DuoPostPersistenceAdapter.save()`가 항상 새 엔티티 생성 후 merge (SELECT+UPDATE 오버헤드) | Medium | **Skip** — 아키텍처 패턴 변경 필요, 현재 트래픽 수준에서는 영향 미미 |
| 4 | `DuoRequestPersistenceAdapter.save()`도 동일한 merge 오버헤드 | Medium | **Skip** — 위와 동일 |
| 5 | `lookupTierInfo()`가 모든 리그 엔트리 조회 후 필터링 | Low | **Skip** — 플레이어당 리그 엔트리 2-3개로 실질적 영향 없음 |
| 6 | `createDuoRequest`의 순차적 DB 호출 (병렬화 가능) | Low | **Skip** — 사용자 트리거 쓰기 작업으로 빈도 낮음 |
| 7 | `RiotRsoClient.fetchPuuid`에서 매 호출마다 config 조회 | Negligible | **Skip** — Map lookup 비용 무시 가능 |

---

## Summary

| File | Change |
|------|--------|
| `DuoService.java` | 매직 스트링 4건 → enum 참조로 교체, 인라인 빌더 → `DuoPostDetailReadModel.of()` 호출 |
| `DuoPostDetailReadModel.java` | `of(DuoPost, boolean, List)` 정적 팩토리 메서드 추가 |
| `DuoPostSearchCommand.java` | `@Setter` 어노테이션 제거 |
| `DuoPostRepositoryCustomImpl.java` | 중복 QueryDSL 로직 → `queryDuoPosts()` 공통 헬퍼 추출, `"ACTIVE"` → `DuoPostStatus.ACTIVE.name()` |

**Build result:** BUILD SUCCESSFUL (106 tasks, 2m 24s)
