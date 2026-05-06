# Simplify Review: RSO PUUID 추가 및 Riot 로그인 차단

**날짜:** 2026-04-13  
**대상 변경:** social_account.puuid 컬럼 추가, Riot Account API 연동, Riot 로그인 차단

---

## 1. 코드 재사용 리뷰

### 발견 사항

| # | 항목 | 판정 |
|---|------|------|
| 1 | `RiotRsoClient`에서 `getProviderConfig("riot")` 이중 호출 — `getUserInfo()`와 `fetchPuuid()` 각각 호출 | **Skip** — HashMap lookup 2회, 무시 가능 수준 |
| 2 | OAuth 클라이언트의 try/catch 에러 래핑 패턴 중복 (`CoreException` rethrow + generic Exception wrap) | **Skip** — 기존 패턴, 이번 변경 범위 밖 |
| 3 | "find member + check withdrawn" 패턴 중복 (`MemberAuthService` vs `MemberProfileService`) | **Skip** — 기존 패턴, 이번 변경 범위 밖 |
| 4 | 인메모리 TTL 저장소 패턴 중복 (`SocialAccountLinkTokenStore`, `CookieOAuth2AuthorizationRequestRepository`) | **Skip** — 기존 패턴, 이번 변경 범위 밖 |

**결론:** 이번 변경에서 기존 유틸리티를 대체할 수 있는 새 코드 작성은 없음.

---

## 2. 코드 품질 리뷰

### 발견 및 조치

| # | 항목 | 심각도 | 조치 |
|---|------|--------|------|
| 1 | `OAuth2AuthenticationSuccessHandler`에서 `ErrorType` FQN 사용 (import 누락) | 높음 | **수정 완료** — import 추가, FQN 2곳 정리 |
| 2 | `userInfo` 로깅 (email, nickname 포함)이 INFO 레벨 | 중간 | **수정 완료** — DEBUG로 변경 |
| 3 | `CustomOidcUserService`에서 PUUID 로깅이 INFO 레벨 | 중간 | **수정 완료** — DEBUG로 변경 |
| 4 | `RiotAccountPort` — 도메인 계층의 유일한 벤더 종속 포트 | 낮음 | **Skip** — Riot만 해당, 실용적 선택 |
| 5 | `RiotRsoClient.getUserInfo()`에서 `providerId`와 `puuid`가 동일 값 | 낮음 | **Skip** — 수동 OAuth 플로우에서 의도된 동작. OIDC 플로우에서는 sub/puuid 분리 |
| 6 | "riot" 문자열 리터럴이 여러 모듈에 산재 | 낮음 | **Skip** — 기존 패턴, 현재 규모에서 합리적 |
| 7 | `SocialAccount.create()` 파라미터 7개 (sprawl) | 낮음 | **Skip** — 도메인 메서드가 application DTO에 의존하지 않는 DDD 패턴 유지 |
| 8 | `isLoginAllowed()`가 데이터 추출 인터페이스에 위치 | 낮음 | **Skip** — provider 2개 수준에서 실용적 |

---

## 3. 효율성 리뷰

### 발견 및 조치

| # | 항목 | 영향 | 조치 |
|---|------|------|------|
| 1 | Riot 직접 로그인 시도 시 `fetchPuuid()` 외부 API 호출 후 즉시 거부 — 불필요한 HTTP 호출 | 낮음 | **Skip** — 프론트엔드 미지원 경로의 비정상 접근에서만 발생. 구조 변경 대비 이점 부족 |
| 2 | `getProviderConfig("riot")` 이중 HashMap lookup | 무시 | **Skip** — 나노초 수준 |
| 3 | `CustomOidcUserService`에서 HashMap 복사 + DefaultOidcUser 생성 | 무시 | **Skip** — 세션당 1회, 정상 |

---

## 수정 요약

| 파일 | 변경 |
|------|------|
| `OAuth2AuthenticationSuccessHandler.java` | `ErrorType` import 추가, FQN → 클래스명 정리 (2곳) |
| `OAuth2AuthenticationSuccessHandler.java` | `userInfo` 로깅 INFO → DEBUG |
| `CustomOidcUserService.java` | PUUID 로깅 INFO → DEBUG |

**빌드 결과:** BUILD SUCCESSFUL
