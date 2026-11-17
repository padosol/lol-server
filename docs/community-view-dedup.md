# 커뮤니티 조회수 중복 방지 설계

> 상태: **설계 확정 대기** · 작성 2026-11-17
> 대상: lol-server `module/domain/community` (+ `member` 의 `SecurityConfig`·`CookieProperties`), lol-ui 앱 진입 세션 검사·커뮤니티 상세 화면
> 기준 코드: lol-server `develop@46d6e786`, lol-ui `develop@a14ebb4`

게시글 상세의 조회수가 새로고침마다, 그리고 한 번 열 때 2~3씩 오르는 문제를 없앤다.
조회수 증가를 상세 GET 에서 떼어내 **별도 조회 기록 API** 로 옮기고, 같은 사람의 반복 조회는
Redis 로 24시간 동안 한 번만 센다. 함께, SSR 상세 조회가 로그인 토큰을 실어 보내게 해서 로그인
사용자의 상세 재조회를 없애고, 앱 진입 시 로그인 세션을 먼저 확정해 토큰이 만료된 채로 들어온
사용자도 회원으로 인식되게 한다.

---

## 1. 배경 — 지금 무엇이 문제인가

### 1.1 새로고침마다 +1

`PostService.getPost` 가 상세 GET 마다 중복 판정 없이 `view_count = view_count + 1` 을 날린다
(`PostService.java:130-131`, `CommunityPostJpaRepository.incrementViewCount`). 같은 사람이 F5 를 누르는 만큼 오른다.

### 1.2 한 번 열면 +2 (운영) / +3 (개발)

상세 페이지 한 번에 상세 GET 이 여러 번 나간다.

| 호출 | 위치 | 인증 |
|---|---|---|
| ① SSR 조회 | lol-ui `loadCommunityData.ts` `loadPostDetail` → `serverApiClient` | 없음 (쿠키·토큰 미전달) |
| ② 개인화 재조회 | lol-ui `PostDetailPanel.tsx` `useEffect` → `refetch()` (로그인 사용자만) | 있음 |
| ③ ②의 중복 | 개발 모드 React StrictMode 가 ② 의 effect 를 두 번 실행 | 있음 |

②가 존재하는 이유: SSR 응답에는 인증이 없어 `currentUserVote`·`currentUserBookmarked` 가 비어 온다.
이 두 값은 vote·bookmark 테이블의 "누가 눌렀나" 데이터라 작성자 id 비교 같은 걸로는 대체할 수 없고,
추천 토글(`PostDetailPanel.tsx` 의 취소/추천 분기)과 북마크 토글이 이 값에 의존한다.

### 1.3 상세 화면이 아닌 곳도 센다

수정 화면(`CommunityEditPageClient`)이 `usePostDetail(postId)` 로 상세 GET 을 부른다. 작성자가 수정 화면을
열기만 해도 +1 이다. 크롤러·링크 미리보기 봇(카카오톡·디스코드)이 페이지를 열어도 SSR ① 이 돌아 +1 이다.

### 1.4 조회수의 성격

조회수는 **표시 전용**이다. `hot_score` 는 투표로만 갱신되고(`VoteService.updateHotScore`) 정렬·랭킹에
조회수는 쓰이지 않는다. 그래서 약간의 과소·과대 집계는 허용되고, 정확한 순방문자 통계는 필요 없다.

---

## 2. 목표 · 비목표 · 반목표

### 목표

- 같은 사람이 같은 글을 **24시간 안에** 다시 봐도 조회수는 1회만 오른다.
- 상세 페이지를 한 번 여는 것은 SSR·재조회·StrictMode 와 무관하게 **최대 1회** 증가다.
- 실제 상세 화면 표시가 아닌 호출(수정 화면, JS 를 실행하지 않는 크롤러)은 세지 않는다.
- 로그인 사용자의 상세 재조회(②)를 정상 경로에서 없앤다.
- **앱 진입 시 로그인 세션을 먼저 확정한다.** access token 이 만료된 채로 들어온 로그인 사용자도 refresh 로 복구되어 추천·북마크 상태가 맞게 보이고, 조회수도 회원 신분으로 1회만 센다.

### 비목표 (이번에 하지 않는다)

- **의도적 어뷰징 방지.** 쿠키를 버려 가며 조회 기록 API 를 반복 호출하는 스크립트, 봇넷 등은 막지 않는다. IP rate limit 도 없다.
- 조회 이력 저장, 순방문자(UV) 통계.
- 상세 외 SSR 호출(목록 등)의 개인화.
- 조회수를 `hot_score` 에 반영.

### 반목표 (기술적으로 돌아가도 실패로 본다)

- 조회수 기록 실패(Redis 장애 등)가 **글 조회 자체를 깨뜨리는 것.**
- 로그인 사용자의 추천·북마크 상태가 **틀리게 보이는 것** — 토큰이 만료된 상태로 들어온 경우 포함.
- PC방처럼 IP 를 공유하는 환경에서 **여러 사람이 한 명으로 합쳐지는 것.** (그래서 IP 로 식별하지 않는다)

---

## 3. 검토한 접근

| 안 | 내용 | 결과 |
|---|---|---|
| **A. 조회 기록 API 분리** | 상세 GET 은 조회만. 브라우저가 상세 화면에 들어올 때 `POST /posts/{id}/views` 를 한 번 보내고, 서버가 Redis 로 24h 중복 제거 후 +1 | **채택** |
| B. GET 에서 계속 세기 | Next 가 SSR 요청에 실제 IP·방문자 쿠키·토큰을 실어 보내고, 서버가 GET 안에서 중복 제거 | 기각 |
| 프론트만 수정 | 중복 호출(①②③)만 제거 | 기각 — 새로고침 반복은 못 막는다 |
| DB 조회 로그 테이블 | `(post_id, viewer, date)` unique 로 중복 판정 | 기각 — 조회마다 insert, 무한히 쌓임. 표시 전용 값에 과하다 |

**A 를 고른 이유**

1. 상세 GET 호출처가 여럿이다(SSR, 토큰 만료 시 재조회, 수정 화면, 크롤러). GET 에서 세면 이것들을 하나하나 걸러야 한다. A 는 "브라우저가 상세 화면을 실제로 띄웠다"는 사실 하나만 센다.
2. 조회 기록 요청은 브라우저 → Ingress → lol-server 로 직접 가므로 로그인 쿠키가 그대로 실리고, 방문자 쿠키도 서버가 응답에 직접 발급할 수 있다. B 는 SSR 경로에서 Next 가 IP·쿠키를 대신 전달하고 서버가 그 헤더를 신뢰하도록 설정을 바꿔야 해 깨지기 쉽다.
3. B 에서는 토큰 만료 시 SSR(비로그인 신분)과 재조회(로그인 신분)가 다른 사람으로 잡혀 다시 +2 가 된다. A 는 GET 이 몇 번 나가든 조회수와 무관하다.
4. 상세 GET 을 컨벤션대로 `@Transactional(readOnly = true)` 로 되돌릴 수 있다.

**A 의 대가** — 페이지당 가벼운 요청 1개가 늘고, SSR 이 그린 숫자는 "내 조회 반영 전" 값이다(POST 응답으로 보정, 5.3절). JS 를 실행하지 않는 클라이언트는 세지 않는데, 이 서비스에선 오히려 바람직하다.

**SSR 개인화(재조회 제거)에 대해 검토한 안**

| 안 | 결과 |
|---|---|
| **Next 서버가 `accessToken` 쿠키를 읽어 SSR 요청에 `Authorization` 으로 실음.** 토큰 만료로 비로그인 응답이 온 경우에만 클라이언트가 (앱 진입 세션 검사로 토큰을 복구한 뒤) 재조회 | **채택** — 백엔드는 이미 요청 헤더/쿠키로 로그인 여부를 판단하므로(`JwtAuthenticationFilter.resolveToken`) 파라미터가 필요 없다 |
| 추천·북마크 상태만 주는 경량 GET(`/posts/{id}/viewer-state`) | 기각 — 엔드포인트가 하나 더 생기고, 요청 수가 그대로다 |
| 조회 기록 POST 응답에 추천·북마크 상태를 같이 담음 | 기각 — 쓰기 API 가 화면용 데이터를 떠안아 나중에 기록 방식을 바꿀 때 묶인다 |

---

## 4. 전체 흐름

```
[브라우저] ──GET 페이지──▶ [Next SSR]
                              │ cookies().accessToken 있으면 Authorization: Bearer 로 전달
                              ▼
                           GET /api/v1/community/posts/{id}
                              → 글 + 개인화 필드 + viewerAuthenticated   (조회수 변화 없음, readOnly)
[브라우저] ◀── HTML(initialPost) ──┘

[앱 진입 — 전체 페이지 로드당 1회, 모든 화면 공통]
  세션 검사: 저장된 user 없음 → status = anonymous (요청 없음)
            저장된 user 있음 → GET /api/v1/members/me
                                 ├ 200            → authenticated
                                 ├ 401 → refresh → 재시도 200 → authenticated (쿠키 갱신됨)
                                 └ refresh 실패   → (한 번 더 확인 후) 조용히 로그아웃 → anonymous

[상세 화면 — status 가 확정된 뒤]
  ├─ POST /api/v1/community/posts/{id}/views        (모든 방문자, 1회)
  │     로그인: viewer = m:{memberId}
  │     비로그인: viewer = v:{visitorId 쿠키}  (없으면 서버가 발급)
  │     Redis SET community:post-view:{id}:{viewer} 1 NX EX 86400
  │       └─ 성공(처음) → DB view_count + 1
  │     응답 { viewCount } → 화면 숫자 보정
  │
  └─ (status == authenticated 인데 initialPost.viewerAuthenticated == false 일 때만)
        GET /api/v1/community/posts/{id}  ← SSR 시점엔 토큰이 만료돼 있었던 경우. 세션 검사가 쿠키를
                                             새로 받아 둔 뒤라 개인화되어 온다. 조회수와 무관
```

---

## 5. 확정 설계

### 5.1 백엔드 — 조회 기록 API

**엔드포인트**: `POST /api/v1/community/posts/{postId}/views`

- 컨트롤러 클래스 매핑이 `{"/api/v1/community", "/api/community"}` 라 옛 경로에도 자동으로 생기지만, 비로그인 허용은 **v1 경로만** 연다(5.4). 이 API 를 옛 경로로 부를 클라이언트가 없다.
- 요청 본문 없음. 인증 선택(로그인 시 `@AuthenticationPrincipal` 로 회원, 아니면 방문자 쿠키).
- 응답 `200 OK` — `ApiResponse<PostViewResponse>`, 본문 `{ "viewCount": 124 }`.
  - 이번 요청으로 셌으면 증가 후 값, 중복이면 현재 값.
  - CLAUDE.md 의 "POST 201" 컨벤션 예외다. 생성되어 주소를 갖는 리소스가 없기 때문이다. (설계자 판단)
- 없는 글·삭제된 글 → 기존과 같이 `POST_NOT_FOUND` (404). `Post.validateNotDeleted()` 재사용.

**처리 순서** (application 서비스, 예: `PostViewService implements PostViewUseCase`)

1. `postPersistencePort.findById` → 없으면 404, `post.validateNotDeleted()`.
2. `PostViewDedupPort.tryMarkViewed(postId, viewer)` — Redis `SET NX EX`.
3. `true`(처음 봄) → `postPersistencePort.incrementViewCount(postId)` 후 `post.getViewCount() + 1` 반환.
   `false`(중복) → `post.getViewCount()` 반환.
4. 트랜잭션은 이 유스케이스에 `@Transactional`. Redis 표시 후 DB 증가가 실패하면 그 조회 1건은 유실된다 — 표시 전용 값이라 허용한다.

**Redis 장애 시**: `tryMarkViewed` 가 예외를 던지면 서비스(또는 어댑터)가 잡아 `warn` 로그를 남기고 **증가하지 않은 현재 값으로 200** 을 준다. 장애 중에 중복 판정 없이 세면 새로고침 부풀리기가 되살아나므로 과소 집계 쪽을 택한다. (설계자 판단)

반환 숫자는 동시 요청이 겹치면 실제 DB 값과 1~2 차이 날 수 있다. 표시 전용이라 별도 재조회는 하지 않는다.

### 5.2 백엔드 — 방문자 식별

| 상태 | viewer 키 | 출처 |
|---|---|---|
| 로그인 | `m:{memberId}` | `@AuthenticationPrincipal AuthenticatedMember` |
| 비로그인 | `v:{visitorId}` | `visitorId` 쿠키 |

**`visitorId` 쿠키**

- 값: UUID v4. 이름 `visitorId`. `HttpOnly`. `Path=/api` (v1·옛 경로 모두 포함, Next 페이지 요청에는 안 실림). `Max-Age` 1년.
- `Secure`·`SameSite`·`Domain` 은 기존 인증 쿠키와 같은 `cookie.*` 설정을 따른다.
- 비로그인 요청에 쿠키가 없거나 UUID 로 파싱되지 않으면 → 새 UUID 를 발급해 `Set-Cookie` 로 내려주고, 그 값으로 판정한다(첫 방문은 센다).
- 로그인 요청에서는 쿠키를 읽지도 발급하지도 않는다.
- 쿠키 읽기·발급은 웹 어댑터(`adapter/in/web`) 책임이다. application 에는 이미 결정된 viewer 값만 넘긴다(값 객체, 예: `domain.vo.ViewerKey` — `ofMember(Long)`, `ofVisitor(UUID)`, `asKey()`).

**`CookieProperties` 위치 이동**: 지금 `member/config/CookieProperties`(prefix `cookie`) 에 있다. community 는 member 의 port.in·application.model 외에는 의존할 수 없으므로(ArchitectureTest), 두 모듈이 함께 쓰도록 **`common/config` 로 옮긴다** (설정값을 여러 모듈이 쓰면 common/config — 기존 프로젝트 규칙). member 쪽은 `AuthCookieManager` 와 테스트 3곳(`SecurityConfigAuthorizationTest`, `AuthCookieManagerTest`, `SecurityConfigTest`)의 import 가 바뀐다. 캐시 값 클래스가 아니므로 Redis flush 는 필요 없다.

**알고 받아들이는 경계 사례**

- 비로그인으로 본 뒤 로그인해서 다시 보면 다른 사람(`v:` → `m:`)으로 한 번 더 센다.
- 로그인 사용자가 여러 기기에서 봐도 1회(`m:` 기준). 토큰이 만료된 채로 들어온 로그인 사용자도 앱 진입 세션 검사(5.6.0)가 끝난 뒤 기록하므로 `m:` 로 센다.
- 작성자가 자기 글을 봐도 센다. 특별 취급하지 않는다.
- 브라우저가 쿠키를 거부하면 비로그인 방문은 매번 센다(비목표 영역).

### 5.3 백엔드 — Redis 키와 포트

- 키: `community:post-view:{postId}:{viewer}` — 예 `community:post-view:42:m:7`, `community:post-view:42:v:0b7c…`
- 값 `"1"`, `SET NX EX 86400`. TTL 은 첫 조회 시점부터 고정이다(NX 라 재조회가 연장하지 않는다). 즉 "같은 사람·같은 글은 첫 조회 후 24시간 동안 1회"다.
- `StringRedisTemplate` 사용(match·member 의 기존 어댑터와 같은 방식). `GenericJackson2JsonRedisSerializer` 를 거치지 않으므로 클래스 이동·리네임 시 역직렬화 문제가 없다.
- 용량: 키당 대략 100B. 하루 10만 조회면 약 10MB, TTL 로 자연 소멸.
- TTL 은 `@ConfigurationProperties` 객체로 둔다(예: `community.view.dedup-ttl: 24h`, community 모듈 config). `@Value` 금지 규칙.
- 포트: `application/port/out/PostViewDedupPort` — `boolean tryMarkViewed(Long postId, ViewerKey viewer)`.
- 어댑터: 예 `adapter/out/cache/PostViewDedupRedisAdapter`. 같은 모듈의 `adapter/out/ratelimit/RedisImageRateLimitAdapter` 가 참고 구현이다(`StringRedisTemplate` 사용, `RuntimeException` 을 잡아 장애를 삼키는 방식).
- `module/domain/community/build.gradle` 에는 업로드 rate limit 용으로 `spring-boot-starter-data-redis` 가 **이미 있다.** 의존 추가는 필요 없다. Redis 연결 설정은 `common` 의 `RedisConfig` 를 그대로 쓴다.

### 5.4 백엔드 — 보안 설정

`SecurityConfig.authorizeRequests` 에서 커뮤니티 경로의 POST 는 전부 `authenticated()` 다(`SecurityConfig.java:126`).
그 규칙 **앞에** 다음을 넣는다.

```java
// 조회 기록은 비로그인 방문자도 남긴다.
.requestMatchers(HttpMethod.POST, "/api/v1/community/posts/*/views").permitAll()
```

CSRF 는 비활성이다. 다른 사이트가 방문자 브라우저로 이 POST 를 위조해도 방문자·글당 24시간에 1회라 영향이 미미하다(비목표 범위).

### 5.5 백엔드 — 상세 GET 정리

- `PostService.getPost` 에서 `incrementViewCount` 두 줄을 제거하고 메서드의 `@Transactional` 을 지워 클래스 기본값 `readOnly = true` 를 따르게 한다.
- 상세 응답(`PostDetailReadModel` → `PostResponse`)에 **`viewerAuthenticated: boolean`** 을 추가한다. 값은 `currentMemberId != null` — "서버가 이 요청을 로그인 사용자로 인식했는가". 생성·수정 응답(`PostDetailResultModel` 경유)은 항상 로그인 상태이므로 `true`.
- `PostPersistencePort.incrementViewCount` 는 조회 기록 서비스가 계속 쓴다.

### 5.6 프론트(lol-ui)

0. **앱 진입 세션 검사** (모든 화면 공통, 사용자 결정 — 8절)
   - **왜 필요한가**: access token(30분)이 만료되면 `accessToken` 쿠키도 같은 Max-Age(1800초)로 사라진다. 상세 GET·조회 기록 POST 는 `permitAll` 이라 토큰이 없어도 401 이 아니라 비로그인으로 성공하므로, `apiClient` 의 401→refresh 가 영영 돌지 않는다. 지금도 30분 넘게 쉬다 상세에 들어오면 추천·북마크 상태가 비어 보인다(**현재 코드에도 있는 문제**). lol-ui 에는 앱 시작 시 인증 필수 API 를 부르는 곳이 없다(`getMyProfile` 은 로그인 직후에만 호출).
   - **위치**: `(main)/[locale]/layout.tsx` 에 `GameDataLoader` 처럼 렌더링 없는 클라이언트 컴포넌트(예: `features/auth` 의 `SessionBootstrap`)를 마운트한다.
   - **상태**: `useAuthStore` 에 `status: "unknown" | "authenticated" | "anonymous"` 를 추가한다. 초기값 `unknown`, persist 대상에서 제외한다(`partialize` 는 지금처럼 `user` 만).
   - **동작** (전체 페이지 로드당 1회, 클라이언트 내비게이션에서는 다시 하지 않음)
     1. 저장된 `user` 없음 → 즉시 `anonymous`. 요청을 보내지 않는다(비로그인 방문자 비용 0).
     2. 저장된 `user` 있음 → `GET /v1/members/me`. 토큰이 없으면 백엔드가 401 을 준다(`/api/v1/members/**` 는 `authenticated`, `RestAuthenticationEntryPoint`).
        - 200 → `setUser(profile)`(프로필 최신화 겸) · `authenticated`.
        - 401 → 기존 인터셉터가 refresh 후 재시도 → 200 이면 위와 같다. refresh 가 새 `accessToken` 쿠키를 심으므로 이후 요청은 모두 회원으로 인식된다.
        - refresh 실패 → **로그인 화면으로 보내지 않는다.** 이 요청에는 인터셉터의 `/login` 이동을 끄는 플래그(예: `skipAuthRedirect`)를 싣는다. 다른 탭이 먼저 refresh 해 토큰이 회전됐을 수 있으므로 `/v1/members/me` 를 **한 번 더** 시도하고, 그래도 401 이면 `clearAuth()` → `anonymous`(조용한 로그아웃). 인터셉터의 `clearAuthStorage()` 는 localStorage 만 지우므로 메모리 상태는 이 컴포넌트가 직접 지운다(`shared` 는 `entities` 를 import 할 수 없다).
        - 네트워크 오류·5xx → 로그인 상태를 바꾸지 않는다. `user` 를 유지한 채 확정된 것으로 보고 진행한다(아래 조회 기록은 보내고, 재조회는 하지 않는다).
   - 로그인 성공(`useGoogleLogin` 등의 `setUser`) 시 `authenticated`, 로그아웃 시 `anonymous` 로 맞춘다.
   - 비용: 로그인 사용자의 전체 페이지 로드당 `/v1/members/me` 1회.

1. **조회 기록 호출**
   - 상세 화면(`PostDetailPanel` 또는 `CommunityDetailPageClient`)에서 **세션 status 가 `unknown` 이 아닐 때** `POST /v1/community/posts/{postId}/views` 를 한 번 보낸다. 비로그인은 즉시 `anonymous` 라 지연이 없고, 로그인 사용자는 세션 검사가 끝난 뒤 보내 토큰이 만료돼 있었더라도 `m:{memberId}` 로 센다. `apiClient`(withCredentials) 로 보내 `visitorId`·인증 쿠키가 실리게 한다.
   - `useRef` 가드로 같은 `postId` 에 한 번만. `postId` 가 바뀌면 다시 보낸다. StrictMode 로 두 번 나가도 서버가 흡수하지만 요청 자체를 줄인다.
   - 응답의 `viewCount` 를 `setQueryData(postDetailKey(postId))` 로 써넣는다. 실패는 조용히 무시한다(토스트 없음, 로그만).
   - 수정 화면(`CommunityEditPageClient`)에서는 호출하지 않는다.
2. **SSR 토큰 전달**
   - `loadPostDetail` 에서 `cookies()` 로 `accessToken` 을 읽어, 있으면 그 요청에만 `Authorization: Bearer <token>` 헤더를 붙여 `serverApiClient` 로 보낸다. 상세 조회에만 적용한다.
   - 운영은 UI·API 가 같은 호스트(`metapick.me`, `/api` 만 lol-server 로 라우팅)이고 `accessToken` 쿠키는 `Path=/` 라 Next 서버에도 도착한다.
   - `serverApiClient` 인터셉터 로그는 method·url 만 찍으므로 토큰이 로그에 남지 않는다. 헤더를 로그에 추가하지 않는다.
   - React `cache` 는 요청 스코프라 사용자 간에 섞이지 않는다. 페이지는 이미 `force-dynamic` 이다.
3. **조건부 재조회**
   - `PostDetailPanel` 의 재조회 effect 조건을 `status === "authenticated" && hasInitialPost && !initialPost.viewerAuthenticated` 로 바꾼다.
   - 해당 경우: SSR 시점엔 access token 이 만료돼 있었다(refresh 쿠키는 `Path=/api/v1/auth/refresh`·옛 경로라 Next 서버에 오지 않아 SSR 에서 갱신할 수 없다). 0번 세션 검사가 refresh 로 쿠키를 새로 받은 뒤라 재조회는 개인화되어 온다.
   - 토큰이 살아 있던 대부분의 경우는 SSR 응답이 이미 개인화돼(`viewerAuthenticated == true`) 재조회가 없다.
4. **타입·주석**
   - `Post` 타입에 `viewerAuthenticated: boolean` 추가.
   - `usePostDetail.ts` 의 "GET 이 조회수를 올린다"는 설명을 갱신한다. "invalidate 하지 않는다" 규칙은 이번에는 유지한다(풀지 여부는 별도 작업).

### 5.7 배포 순서

1. **lol-server 먼저**: 조회 기록 API 추가, 상세 GET 증가 제거, `viewerAuthenticated` 추가, 보안 규칙.
   이 시점부터 lol-ui 배포 전까지는 조회수가 오르지 않는다(과소 집계).
2. **lol-ui 곧바로**: 앱 진입 세션 검사, 조회 기록 호출, SSR 토큰 전달, 조건부 재조회.
   세션 검사는 기존 `/v1/members/me` 만 쓰므로 백엔드 배포와 무관하게 먼저 나가도 된다.

역순(프론트 먼저)은 새 API 가 없어 404, 동시 반영이 어려우면 과대 집계보다 짧은 과소 집계를 택한다. (설계자 판단)
DB 스키마(`lol-db-schema`) 변경 없음. Redis flush 불필요(새 키만 생긴다).

### 5.8 테스트

- **서비스 단위**: 처음 조회 → 증가·증가값 반환 / 중복 → 증가 안 함·현재값 / 없는 글·삭제된 글 → 404 / Redis 예외 → 증가 안 함·예외 전파 안 함.
- **`PostServiceTest`**: 기존 `incrementViewCount` 호출 검증(`PostServiceTest.java:277`)을 "호출하지 않음"으로 뒤집고, `viewerAuthenticated` 값 검증 추가.
- **컨트롤러(WebMvc)**: 비로그인·쿠키 없음 → `Set-Cookie: visitorId` 발급 / 비로그인·쿠키 있음 → 그 값 사용, 재발급 없음 / 잘못된 쿠키 값 → 재발급 / 로그인 → 회원 키 사용, 쿠키 미발급 / 비로그인 POST 가 401 이 아님(보안 규칙).
- **RestDocs**: 새 엔드포인트 스니펫 + `module/app/application/src/docs/asciidoc/api/community/` 에 adoc 추가. 상세 조회는 `community-post-detail.adoc` 는 있지만 이를 만드는 RestDocs 테스트가 지금 없다 — `viewerAuthenticated` 를 문서화하려면 상세 조회 RestDocs 테스트를 새로 작성한다.
- **Redis 어댑터**: NX 동작(두 번째 `false`)과 TTL 설정 확인.
- **ArchitectureTest**: community → common 의존만 늘어나므로 통과해야 한다.
- **lol-ui 세션 검사**: 저장된 user 없음 → 요청 없이 `anonymous` / `/me` 200 → `authenticated` / 401→refresh 성공 → `authenticated` / refresh 실패 → `/login` 이동 없이 재확인 후 `clearAuth`·`anonymous` / 네트워크 오류 → user 유지.
- **lol-ui 상세**: 조회 기록이 status 확정 전에는 나가지 않고 확정 후 1회 / 수정 화면에서는 호출되지 않음 / 재조회 조건 분기 / `loadPostDetail` 이 쿠키가 있을 때만 헤더를 붙임.

---

## 6. 빌더에게 맡기는 것

- 클래스·패키지·필드의 최종 이름(`PostViewService`, `ViewerKey`, `viewerAuthenticated` 등은 제안이다).
- 조회 기록 호출을 `PostDetailPanel` 과 `CommunityDetailPageClient` 중 어디에 둘지.
- Redis 어댑터 테스트 방식(기존 duo 테스트처럼 실제 Redis 를 띄울지, 목으로 할지).
- 조회 기록 유스케이스를 별도 서비스로 둘지, `PostService` 에 메서드로 둘지.

## 7. 설계자가 정한 것 (확인 요청)

| 항목 | 정한 값 | 이유 |
|---|---|---|
| 중복 판정 기간 | 첫 조회 후 24시간 고정 | 새로고침 방지가 목적. 하루 한 번은 다시 세도 된다 |
| 비로그인 식별 | `visitorId` 쿠키, IP 미사용 | PC방 등 IP 공유 환경에서 합쳐지지 않게 |
| 응답 코드 | `200` + `{viewCount}` | 생성되는 리소스가 없다. 화면 숫자 보정에 값이 필요 |
| Redis 장애 시 | 세지 않음, 200 | 장애 중 부풀리기보다 과소 집계 |
| 비로그인 허용 경로 | v1 만 | 옛 경로로 부를 클라이언트가 없다 |
| 작성자 본인 조회 | 센다 | 특별 취급할 요구가 없다 |
| 배포 순서 | 백엔드 → 프론트, 짧은 과소 집계 허용 | 역순은 404 |
| `CookieProperties` | `common/config` 로 이동 | 두 모듈이 쓰는 설정 |
| 세션 검사 API | 기존 `GET /v1/members/me` | 백엔드 변경 없이 401→refresh 를 일으키고 프로필도 최신화된다 |
| 세션 검사 대상 | 저장된 `user` 가 있을 때만 | 비로그인 방문자에게 요청을 늘리지 않는다 |
| refresh 실패 시 | `/login` 이동 없이 조용히 로그아웃 | 글을 읽던 사람을 로그인 화면으로 끌어가지 않는다 |
| refresh 실패 후 재확인 | `/me` 한 번 더 | 다른 탭이 먼저 토큰을 회전시킨 경우의 거짓 로그아웃 방지 |
| 네트워크 오류·5xx 시 | 로그인 상태 유지 | 일시 장애로 로그아웃시키지 않는다 |

## 8. 결정 기록

| 질문 | 결정 | 결정자 |
|---|---|---|
| 조회수 증가 위치 | 별도 조회 기록 API (A안) | 사용자 |
| 로그인 사용자 개인화 | SSR 이 `accessToken` 쿠키를 `Authorization` 으로 전달, 토큰 만료 시에만 재조회 | 사용자 |
| 토큰 만료 사용자의 세션 복구 | **프론트가 직접 복구하되, 앱 진입 시 토큰 검사부터 한다** (5.6.0) | 사용자 |
| 토큰 만료 사용자의 조회 기록 시점 | 세션 검사가 끝난 뒤 보낸다 — `m:` 로 1회만 센다 | 사용자 결정에서 따라옴 |

참고: refresh 는 토큰을 회전시키므로(`MemberAuthService.refreshToken`) 여러 탭이 동시에 refresh 하면 하나가 실패할 수 있다. 앱 전체에 이미 있는 성질이고, 이 설계는 "refresh 실패 후 `/me` 재확인"으로 세션 검사에서의 거짓 로그아웃만 막는다. 탭 간 refresh 직렬화(Web Locks 등)는 범위 밖이다.
