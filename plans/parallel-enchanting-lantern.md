# Application 레이어 @Transactional 정책 변경

## Context

조회 메서드에서 JPA 영속성 컨텍스트의 dirty checking/flush를 피하기 위해, 모든 application 서비스에 클래스 레벨 `@Transactional(readOnly = true)`를 기본 설정하고, 쓰기 메서드에만 `@Transactional`을 오버라이드합니다.

**정책:**
- 클래스 레벨: `@Transactional(readOnly = true)` (기본)
- 쓰기 메서드: `@Transactional` (오버라이드)
- 조회 메서드: 클래스 레벨 상속 → 메서드 레벨 `@Transactional(readOnly = true)` 제거

## 변경 대상

### 1. 클래스 레벨 `@Transactional(readOnly = true)` 추가 (이미 있는 3개 제외, 나머지)

| 파일 | 변경 |
|------|------|
| `ChampionService.java` | 클래스 레벨 `@Transactional(readOnly = true)` 추가 |
| `ChampionStatsService.java` | 클래스 레벨 `@Transactional(readOnly = true)` 추가 |
| `CommentService.java` | 클래스 레벨 `@Transactional(readOnly = true)` 추가 |
| `PostService.java` | 클래스 레벨 `@Transactional(readOnly = true)` 추가 |
| `VoteService.java` | 클래스 레벨 `@Transactional(readOnly = true)` 추가 |
| `LeagueService.java` | 클래스 레벨 `@Transactional(readOnly = true)` 추가 |
| `MatchService.java` | 클래스 레벨 `@Transactional(readOnly = true)` 추가 |
| `MemberAuthService.java` | 클래스 레벨 `@Transactional(readOnly = true)` 추가 |
| `MemberProfileService.java` | 클래스 레벨 `@Transactional(readOnly = true)` 추가 |
| `QueueTypeService.java` | 클래스 레벨 `@Transactional(readOnly = true)` 추가 |
| `RankService.java` | 클래스 레벨 `@Transactional(readOnly = true)` 추가 |
| `SpectatorService.java` | 클래스 레벨 `@Transactional(readOnly = true)` 추가 |
| `SummonerService.java` | 클래스 레벨 `@Transactional(readOnly = true)` 추가 |
| `VersionService.java` | 클래스 레벨 `@Transactional(readOnly = true)` 추가 |

이미 클래스 레벨에 있는 서비스 (변경 없음):
- `PatchNoteService.java` ✅
- `SeasonService.java` ✅
- `TierCutoffService.java` ✅

### 2. 메서드 레벨 `@Transactional(readOnly = true)` 제거 (클래스 레벨에서 상속)

| 파일 | 메서드 |
|------|--------|
| `CommentService.java` | `getComments()` |
| `PostService.java` | `getPosts()`, `searchPosts()`, `getMyPosts()` |
| `MemberProfileService.java` | `getMyProfile()` |
| `SummonerService.java` | `getSummoner()`, `getAllSummonerAutoComplete()`, `getSummonerByPuuid()`, `getRefreshingSummoners()` |

### 3. 쓰기 메서드 `@Transactional` 유지 (변경 없음)

이미 `@Transactional`이 붙어있는 쓰기 메서드들은 그대로 유지:
- `CommentService`: `createComment()`, `updateComment()`, `deleteComment()`
- `PostService`: `createPost()`, `updatePost()`, `deletePost()`, `getPost()` (조회수 증가)
- `VoteService`: `vote()`, `removeVote()`
- `MemberAuthService`: `loginWithOAuth()`, `loginWithOAuthUserInfo()`, `refreshToken()`, `linkSocialAccount()`, `unlinkSocialAccount()`, `logout()`, `withdraw()`
- `MemberProfileService`: `updateNickname()`
- `SummonerService`: `renewalSummonerInfo()`

### 4. `@Component` 클래스 제외

다음은 `@Service`가 아닌 `@Component`이므로 변경 대상에서 제외:
- `SpectatorFinder.java` (캐시/API 조회만 수행)
- `VersionFinder.java` (캐시/DB 조회만 수행)
- `ChampionTierCalculator.java` (유틸리티)

## 검증

- `./gradlew test` 실행하여 기존 테스트 통과 확인
