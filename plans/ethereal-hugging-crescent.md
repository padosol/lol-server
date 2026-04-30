# 도메인 검증 로직을 도메인 객체로 이동

## Context

현재 application 서비스에서 도메인 객체의 boolean 메서드(isOwner, isActive 등)를 호출하고 서비스에서 예외를 던지는 패턴이 19곳에 존재한다. DuoRequest.accept()나 Member.withdraw()는 이미 도메인 내부에서 불변식을 보호하고 있으므로, 나머지도 동일한 원칙을 적용하여 **도메인 규칙 위반은 도메인 객체가 직접 예외를 던지도록** 통일한다.

## 변경 대상: 도메인 객체별 guard 메서드 추가

### 1. DuoPost (`domain/duo/domain/DuoPost.java`)

import 추가: `CoreException`, `ErrorType`

```java
public void validateOwner(Long memberId) {
    if (!this.memberId.equals(memberId)) {
        throw new CoreException(ErrorType.FORBIDDEN);
    }
}

public void validateNotOwner(Long memberId) {
    if (this.memberId.equals(memberId)) {
        throw new CoreException(ErrorType.DUO_POST_SELF_REQUEST);
    }
}

public void validateActive() {
    if (!isActive()) {
        throw new CoreException(ErrorType.DUO_POST_NOT_ACTIVE);
    }
}
```

기존 `isOwner()`, `isActive()` boolean 메서드는 유지 (getDuoPost에서 쿼리용으로 사용 중 — line 122)

### 2. DuoRequest (`domain/duo/domain/DuoRequest.java`)

이미 CoreException import 있음

```java
public void validateRequester(Long memberId) {
    if (!this.requesterId.equals(memberId)) {
        throw new CoreException(ErrorType.FORBIDDEN);
    }
}
```

### 3. Post (`domain/community/domain/Post.java`)

import 추가: `CoreException`, `ErrorType`

```java
public void validateOwner(Long memberId) {
    if (!this.memberId.equals(memberId)) {
        throw new CoreException(ErrorType.FORBIDDEN);
    }
}

public void validateNotDeleted() {
    if (this.deleted) {
        throw new CoreException(ErrorType.POST_NOT_FOUND);
    }
}
```

### 4. Comment (`domain/community/domain/Comment.java`)

import 추가: `CoreException`, `ErrorType`

```java
public void validateOwner(Long memberId) {
    if (!this.memberId.equals(memberId)) {
        throw new CoreException(ErrorType.FORBIDDEN);
    }
}
```

### 5. Member (`domain/member/domain/Member.java`)

이미 CoreException import 있음

```java
public void validateNotWithdrawn() {
    if (this.withdrawnAt != null) {
        throw new CoreException(ErrorType.MEMBER_WITHDRAWN);
    }
}
```

MemberProfileService도 `MEMBER_WITHDRAWN(403)`으로 통일 (기존 `MEMBER_NOT_FOUND(404)` → 변경)

## 변경 대상: 서비스 레이어 (19곳 → guard 호출로 대체)

### DuoService (10곳)

| Line | Before | After |
|------|--------|-------|
| 79-81 | `if (!duoPost.isOwner(memberId)) throw FORBIDDEN` | `duoPost.validateOwner(memberId)` |
| 94-96 | `if (!duoPost.isOwner(memberId)) throw FORBIDDEN` | `duoPost.validateOwner(memberId)` |
| 98-100 | `if (!duoPost.isActive()) throw DUO_POST_NOT_ACTIVE` | `duoPost.validateActive()` |
| 150-152 | `if (!duoPost.isActive()) throw DUO_POST_NOT_ACTIVE` | `duoPost.validateActive()` |
| 154-156 | `if (duoPost.isOwner(memberId)) throw DUO_POST_SELF_REQUEST` | `duoPost.validateNotOwner(memberId)` |
| 190-192 | `if (!duoPost.isOwner(memberId)) throw FORBIDDEN` | `duoPost.validateOwner(memberId)` |
| 206-208 | `if (!duoRequest.isRequester(memberId)) throw FORBIDDEN` | `duoRequest.validateRequester(memberId)` |
| 237-239 | `if (!duoPost.isOwner(memberId)) throw FORBIDDEN` | `duoPost.validateOwner(memberId)` |
| 251-253 | `if (!duoRequest.isRequester(memberId)) throw FORBIDDEN` | `duoRequest.validateRequester(memberId)` |
| 264-266 | `if (!duoPost.isOwner(memberId)) throw FORBIDDEN` | `duoPost.validateOwner(memberId)` |

### PostService (3곳)

| Line | Before | After |
|------|--------|-------|
| 60-62 | `if (!post.isOwner(memberId)) throw FORBIDDEN` | `post.validateOwner(memberId)` |
| 79-81 | `if (!post.isOwner(memberId)) throw FORBIDDEN` | `post.validateOwner(memberId)` |
| 93-95 | `if (post.isDeleted()) throw POST_NOT_FOUND` | `post.validateNotDeleted()` |

### CommentService (2곳)

| Line | Before | After |
|------|--------|-------|
| 72-74 | `if (!comment.isOwner(memberId)) throw FORBIDDEN` | `comment.validateOwner(memberId)` |
| 91-93 | `if (!comment.isOwner(memberId)) throw FORBIDDEN` | `comment.validateOwner(memberId)` |

### MemberAuthService (3곳)

| Line | Before | After |
|------|--------|-------|
| 100-102 | `if (member.isWithdrawn()) throw MEMBER_WITHDRAWN` | `member.validateNotWithdrawn()` |
| 182-184 | `if (member.isWithdrawn()) throw MEMBER_WITHDRAWN` | `member.validateNotWithdrawn()` |
| 217-219 | `if (member.isWithdrawn()) throw MEMBER_WITHDRAWN` | `member.validateNotWithdrawn()` |

### MemberProfileService (1곳)

| Line | Before | After |
|------|--------|-------|
| 48-50 | `if (member.isWithdrawn()) throw MEMBER_NOT_FOUND` | `member.validateNotWithdrawn()` (MEMBER_WITHDRAWN으로 변경) |

## 도메인 단위 테스트 (신규 생성)

테스트 위치: `module/core/lol-server-domain/src/test/java/com/example/lolserver/domain/`

### DuoPostTest — 6개 테스트
- `validateOwner` 성공/실패(FORBIDDEN)
- `validateNotOwner` 성공/실패(DUO_POST_SELF_REQUEST)
- `validateActive` 성공/실패(DUO_POST_NOT_ACTIVE — status!=ACTIVE, expired)

### DuoRequestTest — 2개 테스트
- `validateRequester` 성공/실패(FORBIDDEN)

### PostTest — 4개 테스트
- `validateOwner` 성공/실패(FORBIDDEN)
- `validateNotDeleted` 성공/실패(POST_NOT_FOUND)

### CommentTest — 2개 테스트
- `validateOwner` 성공/실패(FORBIDDEN)

### MemberTest (기존 파일에 추가) — 2개 테스트
- `validateNotWithdrawn` 성공/실패(MEMBER_WITHDRAWN)

## 실행 순서

1. **Member** — 가장 단순, 이미 CoreException import 있음
2. **Comment** — guard 1개
3. **Post** — guard 2개
4. **DuoRequest** — guard 1개, 이미 guard 패턴 존재
5. **DuoPost** — guard 3개, 호출부 가장 많음
6. **검증** — `./gradlew clean build`로 전체 빌드 + 테스트

## 검증

```bash
# 전체 빌드 + 테스트
./gradlew clean build

# 리팩토링 완료 확인: 서비스에 boolean 체크 + throw 패턴 잔존 여부
grep -rn "isOwner\|isActive\|isRequester\|isWithdrawn\|isDeleted" \
  module/core/lol-server-domain/src/main/java/**/application/*.java
# → 결과에 if문+throw 패턴이 없어야 함 (boolean 쿼리 사용만 남아야 함)
```
