# Simplify Review: OAuth 인증 및 회원 도메인

- **날짜**: 2026-03-18
- **브랜치**: `feat/oauth-member-domain`
- **대상 커밋**: `0ff419a feat: OAuth 로그인 및 회원 도메인 기능 추가`

---

## 리뷰 요약

3개의 병렬 리뷰 에이전트 (코드 재사용, 코드 품질, 효율성)가 분석을 수행했으며, 발견된 이슈 중 실질적인 항목을 직접 수정했습니다.

---

## 수정 완료 항목

### 1. [High] 도메인 객체 캡슐화 강화 (Member, RiotAccountLink)

**문제**: `@Setter`로 모든 필드가 외부에 노출되어 도메인 불변성 보장 불가

**수정**:
- `Member.java`: `@Setter` 제거 → `@Builder` 추가 + `Member.create(OAuthUserInfo)` 팩토리 메서드 추가
- `RiotAccountLink.java`: `@Setter` 제거 → `@Builder` 추가 + `RiotAccountLink.create(memberId, riotInfo, platformId)` 팩토리 메서드 추가
- `MemberService.createMember()`: setter 호출 → `Member.create()` 사용
- `MemberService.linkRiotAccount()`: setter 호출 → `RiotAccountLink.create()` 사용
- MapStruct 호환성: `@Builder`를 통해 MapStruct가 자동으로 빌더 전략 사용

### 2. [High] JWT 파싱 중복 제거 (MemberService.refreshToken)

**문제**: `validateToken()` → `getMemberIdFromToken()` 순서로 호출 시 동일한 JWT를 2회 파싱 (암호화 연산 중복)

**수정**:
- `validateToken()` + `getMemberIdFromToken()` → `parseToken()` 한 번으로 통합
- `parseToken()`이 `TokenInfo(memberId, role)` 반환하므로 단일 파싱으로 충분
- 예외 발생 시 `CoreException(ErrorType.INVALID_TOKEN)` 던짐
- 관련 테스트 3개 업데이트: `refreshToken_validToken`, `refreshToken_invalidToken`, `refreshToken_mismatchToken`

---

## 미수정 항목 (참고용)

### 코드 재사용

| 심각도 | 이슈                           | 파일                     | 비고                                                  |
| ------ | ------------------------------ | ------------------------ | ----------------------------------------------------- |
| 낮음   | `StringUtils.hasText()` 미사용 | MemberService.java:56    | null 체크로 충분, 기능적 차이 없음                    |
| 낮음   | `orElse(null)` 패턴            | MemberService.java:69-71 | else 브랜치에 부수 효과가 있어 Optional 체이닝 부적합 |

### 코드 품질

| 심각도 | 이슈                                               | 파일                                                   | 비고                                                          |
| ------ | -------------------------------------------------- | ------------------------------------------------------ | ------------------------------------------------------------- |
| 중간   | Stringly-typed role (`"USER"` 하드코딩)            | Member.java:37                                         | Role enum 도입 시 영속성 계층 전체 변경 필요, 별도 작업 권장  |
| 중간   | `OAuthTokenExchanger.providerName` 파라미터 스프롤 | OAuthTokenExchanger.java:25                            | `ProviderConfig`에 name 필드 추가로 해결 가능, 별도 작업 권장 |
| 중간   | GoogleOAuthClient / RiotRsoClient 코드 중복        | GoogleOAuthClient.java:31-66, RiotRsoClient.java:31-65 | 추상 클래스 추출 가능하나, 현재 2개 구현체로 추상화 시기 아님 |
| 중간   | Request DTO에 Bean Validation 부재                 | AuthController, MemberController                       | `@Valid` + `@NotBlank` 추가 권장, 별도 작업                   |
| 경미   | `OAuthUserInfo`가 Google/Riot 정보 혼합            | OAuthUserInfo.java                                     | 현재 구조로 동작에 문제 없음                                  |
| 경미   | `OAuthStateRedisAdapter`에서 의미 없는 "1" 값 저장 | OAuthStateRedisAdapter.java:19                         | Redis SET 사용 시 값 필수, 관례적 패턴                        |

### 효율성

| 심각도 | 이슈                                                        | 파일                               | 비고                                                                |
| ------ | ----------------------------------------------------------- | ---------------------------------- | ------------------------------------------------------------------- |
| 중간   | TOCTOU: Riot 링크 중복 체크 후 저장                         | MemberService.java:119-125         | DB UNIQUE 제약으로 보완 권장, 별도 마이그레이션 필요                |
| 중간   | 로그인 시 매번 lastLogin UPDATE                             | MemberService.java:76-77           | 비동기 업데이트 또는 배치 전환 가능, 현재 부하 수준에서는 수용 가능 |
| 경미   | `JwtAuthenticationFilter` AUTHORITY_CACHE 미스 시 객체 생성 | JwtAuthenticationFilter.java:50-52 | USER/ADMIN만 사용하므로 실질적 미스 없음                            |
| 경미   | `generateTokens()`에서 expiry 메서드 중복 호출              | MemberService.java:165-172         | 캐시된 property 접근이므로 성능 영향 미미                           |

---

## 빌드 결과

```
BUILD SUCCESSFUL in 3m
93 actionable tasks: 33 executed, 60 up-to-date
```

모든 테스트 통과 확인.
