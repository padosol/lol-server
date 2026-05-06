# Simplify Review: Duo Magic String Refactor

**Date:** 2026-04-14
**Target:** 커밋 `a8ee5bf` — 듀오 서비스 매직 스트링 제거 및 QueryDSL 중복 로직 추출

---

## 1. Code Reuse Review

### Findings

| # | Item | Action |
|---|------|--------|
| 1 | `DuoPostDetailReadModel.of()` 팩토리 메서드 패턴이 기존 `DuoPostReadModel.of()`, `DuoRequestReadModel.of()` 등과 일관됨 | **Skip** — 문제 없음, 기존 패턴 준수 |
| 2 | QueryDSL `queryDuoPosts` 추출 패턴은 이 리포지토리에서만 사용됨. `CommunityPostRepositoryCustomImpl`에도 유사한 중복이 있으나 join/orderBy 차이로 추출이 덜 깔끔함 | **Skip** — 낮은 우선순위, 별도 리팩토링 대상 |
| 3 | `RiotOAuth2UserInfoExtractor`에 `"RIOT"` 매직 스트링 잔존 (`OAuthProvider.RIOT.name()` 사용 가능) | **Skip** — 이번 커밋 범위 밖 (infra:api), 별도 작업 필요 |
| 4 | `GoogleOAuth2UserInfoExtractor`에 `"GOOGLE"` 매직 스트링 잔존 | **Skip** — 동일 사유 |
| 5 | `RankPersistenceAdapter.toQueueString()`에 `"RANKED_SOLO_5x5"`, `"RANKED_FLEX_SR"` 매직 스트링 잔존 (`QueueType` enum 사용 가능) | **Skip** — 이번 커밋 범위 밖, 별도 작업 필요 |
| 6 | `MatchSummonerRepositoryCustomImpl`에 하드코딩된 queue ID `420`, `440` (`QueueType.getQueueId()` 사용 가능) | **Skip** — 이번 커밋 범위 밖, 별도 작업 필요 |
| 7 | 변경된 파일(`DuoService.java`, `DuoPostRepositoryCustomImpl.java`) 내 잔여 매직 스트링 없음 | **Skip** — 문제 없음, 모두 제거 완료 |

---

## 2. Code Quality Review

### Findings

| # | Item | Severity | Action |
|---|------|----------|--------|
| 1 | `.map(SocialAccount::getPuuid)` 메서드 레퍼런스가 `.map(sa -> sa.getPuuid())` 람다로 회귀 — `SocialAccount` import 제거 시 함께 변경된 것으로 보임 | Low | **Fixed** — 메서드 레퍼런스 복원 및 `SocialAccount` import 추가 |
| 2 | `DuoMatchResultReadModel`에 `of()` 팩토리 메서드 없음 — `acceptDuoRequest`와 `confirmDuoRequest`에서 인라인 빌더 중복 | Medium | **Skip** — 이번 커밋에서 변경하지 않은 코드, 별도 리팩토링 대상 |
| 3 | `SocialAccount.provider` 필드가 `String` 타입이나 `OAuthProvider` enum 존재 (stringly-typed) | Medium | **Skip** — 영속성 계층 변경 필요한 큰 리팩토링, 별도 작업 추적 권장 |
| 4 | 테스트 코드(`DuoServiceTest`)에 `"ACTIVE"`, `"RIOT"` 등 매직 스트링 잔존 | Low | **Skip** — 테스트 코드는 이번 리팩토링 범위 밖, 별도 개선 권장 |
| 5 | `@Setter` 제거(`DuoPostSearchCommand`) — 코드베이스 전체에서 setter 호출 없음, 안전함 | None | **Skip** — 문제 없음 |
| 6 | `DuoPostDetailReadModel` → `DuoPost` 의존성 방향 (application → domain) — 아키텍처 규칙 준수 | None | **Skip** — 문제 없음 |
| 7 | QueryDSL 중복 제거 (`queryDuoPosts`) — 깔끔하게 추출됨, `null` BooleanExpression 처리 정상 | None | **Skip** — 문제 없음 |

---

## 3. Efficiency Review

### Findings

| # | Item | Impact | Action |
|---|------|--------|--------|
| 1 | `lookupTierInfo`가 모든 리그를 로드 후 Java에서 필터링 — 쿼리 레벨 필터(`findByPuuidAndQueue`) 가능 | Low | **Skip** — `league_summoner` 테이블 unique 제약으로 puuid당 최대 3행, 성능 영향 무시 가능 |
| 2 | `queryDuoPosts` varargs에 null `BooleanExpression` 전달 — QueryDSL `.where()`에서 null 무시 처리 | Negligible | **Skip** — 문제 없음, QueryDSL 표준 패턴 |
| 3 | `LocalDateTime.now()` 쿼리 내 평가 시점 — 메서드 호출마다 즉시 평가, 캐싱/재사용 위험 없음 | Negligible | **Skip** — 문제 없음 |
| 4 | `member.getSocialAccounts()` N+1 가능성 — `findByIdWithSocialAccounts`에서 `LEFT JOIN FETCH` 사용, 단일 쿼리 | Negligible | **Skip** — 문제 없음, 이미 최적화됨 |
| 5 | 무제한 데이터 구조 — 모든 컬렉션이 bounded (리그 3행, 소셜 계정 수개, Slice 페이지네이션) | Negligible | **Skip** — 문제 없음 |

---

## Summary

| File | Change |
|------|--------|
| `DuoService.java` | `.map(sa -> sa.getPuuid())` → `.map(SocialAccount::getPuuid)` 메서드 레퍼런스 복원, `SocialAccount` import 추가 |

**Build result:** BUILD SUCCESSFUL

---

## Follow-up Recommendations

이번 커밋 범위 밖이지만 향후 개선 가능한 항목:

1. **infra 매직 스트링 제거**: `RiotOAuth2UserInfoExtractor`, `GoogleOAuth2UserInfoExtractor`, `RankPersistenceAdapter`, `MatchSummonerRepositoryCustomImpl`에 잔존하는 매직 스트링을 `OAuthProvider`/`QueueType` enum으로 교체
2. **`DuoMatchResultReadModel.of()` 팩토리 메서드 추출**: `acceptDuoRequest`/`confirmDuoRequest` 인라인 빌더 중복 제거
3. **`SocialAccount.provider` 타입 변경**: `String` → `OAuthProvider` enum (영속성 계층 변경 수반)
4. **테스트 코드 매직 스트링 제거**: `DuoServiceTest` 등에서 enum `.name()` 사용으로 통일
