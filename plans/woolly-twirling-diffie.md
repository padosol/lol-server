# /simplify 코드 리뷰 수정 계획

## Context

Member Aggregate Root DDD 리팩토링 + 회원 탈퇴 기능 구현 후 코드 리뷰를 진행했다.
3개 리뷰 에이전트(Code Quality, Efficiency, Code Reuse)의 발견사항 중 실제 수정이 필요한 항목을 정리한다.

---

## 수정 항목 (우선순위순)

### 1. `@EqualsAndHashCode` 수정 — mutable 필드 사용 제거
**파일**: `SocialAccount.java`
**문제**: `@EqualsAndHashCode(of = {"provider", "providerId"})` — `anonymize()`에서 `providerId`를 변경하므로 hash 계약 위반 위험
**수정**: `@EqualsAndHashCode(of = "id")` 또는 `@EqualsAndHashCode` 제거

### 2. 파라미터 스프롤 제거 — `OAuthUserInfo`를 도메인 메서드에 직접 전달
**파일**: `Member.java`
**문제**: `linkSocialAccount(String, String, String, String, String)`, `createNewWithSocialAccount(String, String, String, String, String)` — 5개 raw String 파라미터. `OAuthUserInfo`가 이미 동일한 값을 캡슐화함
**수정**: `linkSocialAccount(OAuthUserInfo)`, `createNewWithSocialAccount(OAuthUserInfo)` 시그니처로 변경. `MemberAuthService`에서 unpacking 보일러플레이트 제거

### 3. `findByIdWithSocialAccounts` fetch join 적용
**파일**: `MemberJpaRepository.java`, `MemberPersistenceAdapter.java`
**문제**: `findById()` → lazy load 트리거 → 매번 2개 쿼리 발생
**수정**: `@Query("SELECT m FROM MemberEntity m LEFT JOIN FETCH m.socialAccounts WHERE m.id = :id")` 추가

### 4. `findOrCreateMemberAndGenerateTokens` 메서드 분리
**파일**: `MemberAuthService.java`
**문제**: 40줄+ 메서드에 5가지 관심사 혼재
**수정**: 기존 회원 로그인과 신규 회원 생성 로직을 분리
  - `findExistingMember(SocialAccount)` — 탈퇴 검증 포함
  - `createNewMember(OAuthUserInfo)` — 재가입 제한 검증 포함

### 5. "find member + check withdrawn" 반복 패턴 추출
**파일**: `MemberAuthService.java`
**문제**: `findByIdWithSocialAccounts(memberId).orElseThrow(MEMBER_NOT_FOUND)` 패턴이 `linkSocialAccount`, `unlinkSocialAccount`, `withdraw` 3곳에 반복
**수정**: `findMemberWithSocialAccounts(Long memberId)` private 헬퍼 추출 (MemberProfileService의 `findActiveMember` 패턴과 동일)

### 6. `MemberPersistenceAdapter.save()` 에러 메시지 개선
**파일**: `MemberPersistenceAdapter.java`
**문제**: `orElseThrow()` — 메시지 없이 `NoSuchElementException` 발생
**수정**: `orElseThrow(() -> new IllegalStateException("Member entity not found for id: " + member.getId()))`

---

## 수정 제외 (false positive / 허용)

- **`UUID.randomUUID()` 테스트 불가**: withdraw()의 닉네임 랜덤값. `startsWith("탈퇴한회원_")`로 테스트 가능, Clock 주입은 오버엔지니어링
- **`OAuthProvider` enum 미사용**: provider를 String으로 사용하는 것은 기존 설계. 범위가 큰 리팩토링이므로 별도 작업
- **8개 의존성**: MemberAuthService의 역할 범위상 허용 가능
- **N개 개별 INSERT (withdraw)**: 소셜 계정 최대 2-3개, 탈퇴는 희귀 작업
- **syncSocialAccounts O(n*m)**: n=2-3으로 무시 가능
- **L1 캐시 재조회**: 같은 트랜잭션 내 L1 캐시 히트

## 수정 대상 파일

| 파일 | 변경 |
|------|------|
| `domain/member/domain/SocialAccount.java` | `@EqualsAndHashCode` 수정 |
| `domain/member/domain/Member.java` | 메서드 시그니처 변경 (OAuthUserInfo 전달) |
| `domain/member/application/MemberAuthService.java` | 헬퍼 추출 + 메서드 분리 + 파라미터 수정 |
| `repository/member/repository/MemberJpaRepository.java` | fetch join 쿼리 추가 |
| `repository/member/adapter/MemberPersistenceAdapter.java` | fetch join 사용 + 에러 메시지 |
| 테스트 파일 | 시그니처 변경 반영 |

## 검증

`./gradlew clean build` — 전체 빌드 + 테스트 통과
