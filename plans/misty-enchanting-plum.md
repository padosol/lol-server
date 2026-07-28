# 반복 리팩토링 패턴 일괄 정리

## Context

최근 커밋들(`a8ee5bf`, `47aac18`, `2dad57b` 등)에서 매직 스트링 제거와 ReadModel.of() 추출이 반복적으로 진행되고 있다. infra 계층에 아직 잔여 매직 스트링 6건, 인라인 빌더 3곳이 남아 있어 동일 패턴의 커밋이 계속 발생할 수 있다. 한 번에 일괄 정리하여 반복 작업을 종료한다.

---

## Step 1: 매직 스트링 제거 (4개 파일, 6건)

모든 변경은 독립적이므로 병렬 수정 가능.

### 1a. OAuthProvider 매직 스트링 (2건)

**`module/infra/api/.../controller/security/oauth2/RiotOAuth2UserInfoExtractor.java`**
- L27: `.provider("RIOT")` → `.provider(OAuthProvider.RIOT.name())`
- import 추가: `com.example.lolserver.domain.member.domain.vo.OAuthProvider`

**`module/infra/api/.../controller/security/oauth2/GoogleOAuth2UserInfoExtractor.java`**
- L22: `.provider("GOOGLE")` → `.provider(OAuthProvider.GOOGLE.name())`
- import 추가: 동일

### 1b. QueueType 매직 스트링 (2건)

**`module/infra/persistence/postgresql/.../repository/rank/adapter/RankPersistenceAdapter.java`**
- L63: `case SOLO -> "RANKED_SOLO_5x5"` → `case SOLO -> QueueType.RANKED_SOLO_5x5.name()`
- L64: `case FLEX -> "RANKED_FLEX_SR"` → `case FLEX -> QueueType.RANKED_FLEX_SR.name()`
- import 추가: `com.example.lolserver.QueueType`

### 1c. 하드코딩 Queue ID (2건)

**`module/infra/persistence/postgresql/.../repository/match/matchsummoner/dsl/impl/MatchSummonerRepositoryCustomImpl.java`**
- L245: `.eq(420)` → `.eq(QueueType.RANKED_SOLO_5x5.getQueueId())`
- L246: `.eq(440)` → `.eq(QueueType.RANKED_FLEX_SR.getQueueId())`
- import 추가: `com.example.lolserver.QueueType`

---

## Step 2: ReadModel.of() 팩토리 메서드 추출 (3건)

ReadModel 파일 먼저 수정 후, Service 파일에서 호출부 교체.

### 2a. DuoMatchResultReadModel

**ReadModel:** `module/core/lol-server-domain/.../domain/duo/application/model/DuoMatchResultReadModel.java`

오버로드된 2개 팩토리 메서드 추가:
- `of(DuoPost, DuoRequest)` — accept 케이스 (partnerInfo = null, status는 duoRequest.getStatus()에서 가져옴)
- `of(DuoPost, DuoRequest, Summoner)` — confirm 케이스 (nullable Summoner에서 gameName/tagLine 추출)

import 추가: `DuoPost`, `DuoRequest`, `Summoner`

**Service:** `module/core/lol-server-domain/.../domain/duo/application/DuoService.java`
- L171-177 (`acceptDuoRequest`): 인라인 빌더 → `DuoMatchResultReadModel.of(duoPost, duoRequest)`
- L202-217 (`confirmDuoRequest`): partnerGameName/tagLine 추출 로직 + 인라인 빌더 → `Summoner partnerSummoner = ...` + `DuoMatchResultReadModel.of(duoPost, duoRequest, partnerSummoner)`

> **검증:** `accept()` 호출 후 `duoRequest.getStatus()` = `ACCEPTED`, `confirm()` 호출 후 = `CONFIRMED` (DuoRequest.java L64, L72 확인 완료)

### 2b. PostDetailReadModel

**ReadModel:** `module/core/lol-server-domain/.../domain/community/application/model/PostDetailReadModel.java`

팩토리 메서드 추가:
- `of(Post, Member, Vote)` — currentUserVote null 처리 포함

import 추가: `Post`, `Vote`, `Member`

**Service:** `module/core/lol-server-domain/.../domain/community/application/PostService.java`
- `toDetailReadModel()` private 메서드 삭제 (L137-155)
- L50, L71: `toDetailReadModel(saved, member, null)` → `PostDetailReadModel.of(saved, member, null)`
- L111: `toDetailReadModel(post, member, currentUserVote)` → `PostDetailReadModel.of(post, member, currentUserVote)`
- `AuthorReadModel` import 제거 (더 이상 직접 참조 없음)

### 2c. CommentTreeReadModel

**ReadModel:** `module/core/lol-server-domain/.../domain/community/application/model/CommentTreeReadModel.java`

변경 사항:
1. `@Setter` 제거 + `import lombok.Setter` 제거 (children 필드 — `setChildren()`는 코드베이스 어디서도 호출되지 않음, `parent.getChildren().add(node)`만 사용)
2. `DELETED_COMMENT_CONTENT` 상수 추가 (CommentService에서 이동)
3. `of(Comment, Member)` 팩토리 메서드 추가 — 삭제된 댓글 content 치환 + nullable author 처리

import 추가: `Comment`, `Member`

**Service:** `module/core/lol-server-domain/.../domain/community/application/CommentService.java`
- `DELETED_COMMENT_CONTENT` 상수 삭제 (L33)
- `toReadModel()` private 메서드 삭제 (L141-162)
- L66, L85: `toReadModel(saved, member)` → `CommentTreeReadModel.of(saved, member)`
- L121: `toReadModel(comment, member)` → `CommentTreeReadModel.of(comment, member)`
- `AuthorReadModel` import 제거

---

## Step 3: 검증

```bash
./gradlew clean build
```

기존 테스트가 모든 변경의 동작 동등성을 커버함:
- `DuoServiceTest` — acceptDuoRequest(status=ACCEPTED, null partner), confirmDuoRequest(gameName, tagLine, status=CONFIRMED)
- `PostServiceTest` — createPost, updatePost, getPost → PostDetailReadModel 검증
- `CommentServiceTest` — createComment, updateComment, getComments 트리 빌딩 검증

새 테스트 불필요 (순수 구조 리팩토링).

---

## 수정 파일 목록 (10개)

| # | 파일 | 변경 내용 |
|---|------|----------|
| 1 | `RiotOAuth2UserInfoExtractor.java` | `"RIOT"` → `OAuthProvider.RIOT.name()` |
| 2 | `GoogleOAuth2UserInfoExtractor.java` | `"GOOGLE"` → `OAuthProvider.GOOGLE.name()` |
| 3 | `RankPersistenceAdapter.java` | `"RANKED_SOLO_5x5"`, `"RANKED_FLEX_SR"` → `QueueType.name()` |
| 4 | `MatchSummonerRepositoryCustomImpl.java` | `420`, `440` → `QueueType.getQueueId()` |
| 5 | `DuoMatchResultReadModel.java` | `of()` 팩토리 2개 추가 |
| 6 | `DuoService.java` | 인라인 빌더 → `DuoMatchResultReadModel.of()` |
| 7 | `PostDetailReadModel.java` | `of()` 팩토리 추가 |
| 8 | `PostService.java` | `toDetailReadModel()` 제거, `PostDetailReadModel.of()` 호출 |
| 9 | `CommentTreeReadModel.java` | `@Setter` 제거, `of()` 팩토리 추가, 상수 이동 |
| 10 | `CommentService.java` | `toReadModel()` 제거, `CommentTreeReadModel.of()` 호출 |
