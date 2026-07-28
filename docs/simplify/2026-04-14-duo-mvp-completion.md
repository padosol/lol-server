# Simplify Review: Duo MVP Completion

**Date:** 2026-04-14
**Target:** 듀오 찾기 기능 MVP 완성 — 게시글 수정 API 추가 및 RestDocs 테스트 13개 작성

---

## 1. Code Reuse Review

### Findings

| # | Item | Action |
|---|------|--------|
| 1 | `UpdateDuoPostCommand`와 `CreateDuoPostCommand`가 동일 필드 — 합칠 수 있는지 | **Skip** — 커뮤니티 도메인도 Create/Update Command를 분리 유지. 생성과 수정은 의미적으로 다른 연산이며, 향후 Create에만 필요한 필드 추가 가능성 |
| 2 | `UpdateDuoPostRequest`와 `CreateDuoPostRequest` record 동일 구조 | **Skip** — 각각 다른 타입의 Command를 반환하는 `toCommand()` 메서드를 가지므로 합치면 복잡해짐 |
| 3 | `DuoService.updateDuoPost()`의 findById/isOwner 패턴이 deleteDuoPost와 중복 | **Skip** — 3줄 수준의 중복이며 후속 검증이 메서드마다 다름. 과도한 추상화 회피 |
| 4 | RestDocs responseFields가 두 테스트 클래스 간 중복 | **Skip** — result/errorMessage 래퍼 3줄만 겹치며 data 필드는 다름. 문서화 테스트는 독립성 유지가 적절 |

---

## 2. Code Quality Review

### Findings

| # | Item | Severity | Action |
|---|------|----------|--------|
| 1 | DuoServiceTest에서 `matchedPost_throwsNotActive`와 `expiredPost_throwsNotActive`가 `DuoPost.builder()`를 인라인 중복 작성 (~30행) | Medium | **Fixed** — `createTestDuoPost(id, memberId, status, expiresAt)` 오버로드 헬퍼 추가, 인라인 빌더를 1줄 호출로 교체 |
| 2 | `UpdateDuoPostCommand`가 Lane을 String으로 받아 서비스에서 파싱 | Low | **Skip** — `CreateDuoPostCommand`와 동일 패턴, 프로젝트 전체 일관성 유지 |
| 3 | `updateDuoPost` RestDocs 응답이 `subsectionWithPath("data")` 사용 | Low | **Skip** — `CommunityPostControllerTest.updatePost`와 동일 프로젝트 패턴 |
| 4 | `deleteDuoPost`에 `isActive()` 검증 없음 | Low | **Skip** — 이번 변경 범위 밖 (기존 동작), 삭제는 어떤 상태에서든 허용하는 것이 의도적 설계로 판단 |

---

## 3. Efficiency Review

### Findings

| # | Item | Impact | Action |
|---|------|--------|--------|
| 1 | `updateContent()`가 값 변경 여부 미확인, 동일 내용 수정 시 불필요한 DB write | Negligible | **Skip** — MVP 단계 트래픽에서 무시할 수 있는 수준. 향후 트래픽 증가 시 early return 추가 권장 |
| 2 | `parseLane()` 2회 호출 | Negligible | **Skip** — `Lane.valueOf()` 래퍼 수준, 비용 무시 가능 |
| 3 | `isActive()`의 `LocalDateTime.now()` 호출 | Negligible | **Skip** — 나노초 수준 시계 호출 1회, 문제 없음 |

---

## Summary

| File | Change |
|------|--------|
| `DuoServiceTest.java` | `createTestDuoPost(id, memberId, status, expiresAt)` 오버로드 헬퍼 추가, 2개 테스트의 인라인 빌더를 헬퍼 호출로 교체 |

**Build result:** BUILD SUCCESSFUL
