# Simplify Review: @Transactional readOnly Policy

**Date:** 2026-04-13
**Target:** 모든 application 서비스에 클래스 레벨 `@Transactional(readOnly = true)` 적용 및 메서드 레벨 중복 제거

---

## 1. Code Reuse Review

### Findings

| # | Item | Action |
|---|------|--------|
| 1 | `api-local.yml`의 `riot.account-uri`가 `americas`로 남아있어 `oauth-local.yml`의 `asia`와 불일치 (stale config 가능성) | **Skip** — 기존 변경 사항, 이번 작업 범위 외 |
| 2 | `RiotRsoClient`에서 `getProviderConfig("riot")`가 `getUserInfo()`와 `fetchPuuid()` 양쪽에서 호출됨 | **Skip** — HashMap lookup으로 영향 미미, 기존 코드 |

---

## 2. Code Quality Review

### Findings

| # | Item | Severity | Action |
|---|------|----------|--------|
| 1 | `RiotRsoClient.getUserInfo()`는 providerId=puuid로 설정하지만, `RiotOAuth2UserInfoExtractor`는 providerId=sub(OIDC subject)로 설정 — 두 코드 경로에서 providerId 값이 다름 | High | **Skip** — 기존 puuid 작업의 설계 의도 확인 필요, 이번 @Transactional 작업 범위 외 |
| 2 | `Member.linkSocialAccount()`, `SocialAccount.create()` 등 6~7개 String 파라미터 — parameter sprawl | Medium | **Skip** — 기존 코드, Value Object 도입은 별도 리팩토링 |
| 3 | `"riot"`(소문자)와 `"RIOT"`(대문자), `OAuthProvider.RIOT` enum이 혼용됨 — stringly-typed | Low | **Skip** — 기존 코드, 별도 정리 필요 |
| 4 | `RiotAccountPort`가 domain 포트에 위치하지만 infra 컴포넌트(`CustomOidcUserService`)에서만 사용 — leaky abstraction | Medium | **Skip** — 아키텍처 결정 사항, 별도 논의 필요 |
| 5 | `RiotRsoClient`에서 config lookup 중복 | Low | **Skip** — 영향 미미 |

---

## 3. Efficiency Review

### Findings

| # | Item | Impact | Action |
|---|------|--------|--------|
| 1 | `ChampionService`, `SpectatorService` 등 Redis/HTTP-only 서비스에 `@Transactional(readOnly = true)` 적용 시 불필요한 DB 커넥션 점유 가능성 | Medium | **Skip** — 사용자가 명시적으로 모든 서비스에 적용을 요청함. 향후 connection pool 이슈 발생 시 선택적 제외 고려 |
| 2 | `ChampionStatsService`는 ClickHouse만 사용하지만 PostgreSQL TransactionManager에 바인딩됨 | Medium | **Skip** — 위와 동일 사유 |
| 3 | `ChampionStatsService.buildPositionStats()`에서 7+3개의 독립 쿼리를 순차 실행 — 병렬화 가능 | Medium | **Skip** — 기존 코드, 이번 작업 범위 외 |
| 4 | `CustomOidcUserService`의 HashMap 복사 — Riot 로그인에만 발생, 소규모 claims | Negligible | **Skip** — 문제 없음 |

---

## Summary

| File | Change |
|------|--------|
| `ChampionService.java` | 클래스 레벨 `@Transactional(readOnly = true)` 추가 |
| `ChampionStatsService.java` | 클래스 레벨 `@Transactional(readOnly = true)` 추가 |
| `CommentService.java` | 클래스 레벨 추가, 메서드 레벨 `readOnly` 제거 |
| `PostService.java` | 클래스 레벨 추가, 메서드 레벨 `readOnly` 3개 제거 |
| `VoteService.java` | 클래스 레벨 `@Transactional(readOnly = true)` 추가 |
| `LeagueService.java` | 클래스 레벨 `@Transactional(readOnly = true)` 추가 |
| `MatchService.java` | 클래스 레벨 `@Transactional(readOnly = true)` 추가 |
| `MemberAuthService.java` | 클래스 레벨 `@Transactional(readOnly = true)` 추가 |
| `MemberProfileService.java` | 클래스 레벨 추가, 메서드 레벨 `readOnly` 제거 |
| `QueueTypeService.java` | 클래스 레벨 `@Transactional(readOnly = true)` 추가 |
| `RankService.java` | 클래스 레벨 `@Transactional(readOnly = true)` 추가 |
| `SpectatorService.java` | 클래스 레벨 `@Transactional(readOnly = true)` 추가 |
| `SummonerService.java` | 클래스 레벨 추가, 메서드 레벨 `readOnly` 4개 제거 |
| `VersionService.java` | 클래스 레벨 `@Transactional(readOnly = true)` 추가 |

**Build result:** BUILD SUCCESSFUL (52 actionable tasks, `./gradlew test`)
