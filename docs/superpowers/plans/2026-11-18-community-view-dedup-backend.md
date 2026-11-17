# 커뮤니티 조회수 중복 방지 — lol-server 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 상세 GET 에서 조회수 증가를 떼어내고, `POST /api/v1/community/posts/{postId}/views` 가 같은 방문자의 반복 조회를 Redis 로 24시간 동안 1회만 세게 한다.

**Architecture:** community 컨텍스트에 조회 기록 유스케이스(`PostViewUseCase`)와 Redis 중복 판정 포트(`PostViewDedupPort`)를 추가한다. 웹 어댑터가 로그인 회원(`m:{memberId}`) 또는 `visitorId` 쿠키(`v:{uuid}`)로 방문자를 정하고, 상세 GET 은 읽기 전용으로 돌리며 `viewerAuthenticated` 를 응답에 싣는다.

**Tech Stack:** Spring Boot 3.3, JDK 21, Spring Data Redis(`StringRedisTemplate`), Spring Security, JUnit 5 + Mockito + AssertJ, Spring REST Docs.

**Spec:** `docs/community-view-dedup.md` (같은 브랜치). 프론트 계획: `docs/superpowers/plans/2026-11-18-community-view-dedup-frontend.md`.

## Global Constraints

- 작업 위치: lol-server 저장소, `develop` 기준 브랜치(예: `feature/community-view-dedup`). 브랜치·커밋·PR 제목에 `MP-*` 를 넣지 않는다(Linear ID 는 PR 본문 `Closes MP-…` 로만).
- 커밋 메시지: `<type>: <한글 설명>` (`feat`·`fix`·`refactor`·`docs`·`chore`·`test`).
- 의존 규칙(`ArchitectureTest`): community 의 domain·application 은 `org.springframework.web..`·`jakarta.persistence..` 등 인프라 타입에 의존 금지. community 는 member 의 port.in·application.model 외 의존 금지. `adapter.in` 은 `adapter.out` 에 의존 금지.
- 설정값은 `@ConfigurationProperties` 객체로 묶고 `@Value` 를 쓰지 않는다.
- DI 는 `@RequiredArgsConstructor` + `private final`.
- 엔드포인트: `POST /api/v1/community/posts/{postId}/views`, 요청 본문 없음, 응답 `200` + `ApiResponse<PostViewResponse>` = `{ "viewCount": <int> }`. v1 경로만 연다.
- Redis 키 `community:post-view:{postId}:{viewer}`, 값 `"1"`, `SET NX` + TTL. TTL 은 `community.view.dedup-ttl`, 기본 `24h`.
- viewer 문자열: 회원 `m:{memberId}`, 비로그인 `v:{visitorId}`.
- 방문자 쿠키: 이름 `visitorId`, 값 UUID(소문자 36자 표준형), `HttpOnly`, `Path=/api`, `Max-Age=31536000`(365일), `Secure`·`SameSite`·`Domain` 은 `cookie.*`(`CookieProperties`).
- Redis 장애 시 세지 않고 200(현재 값). 없는 글·삭제된 글은 `POST_NOT_FOUND`(404).
- 상세 GET(`PostService.getPost`)은 조회수를 바꾸지 않고 `@Transactional(readOnly = true)`(클래스 기본값)로 동작한다.
- 상세 응답 `viewerAuthenticated` = 서버가 이 요청을 로그인 회원으로 인식했는가(`currentMemberId != null`). 생성·수정 응답은 `true`.
- 검증 명령: `./gradlew :module:domain:community:test`, `./gradlew :module:domain:member:test`, `./gradlew compileJava compileTestJava`, `./gradlew archTest`.

## Review Focus

1. **같은 방문자의 동시 첫 조회(두 탭에서 거의 동시에 POST)** — 증가는 1회여야 한다. 원자성은 `SET NX` 가 보장하고, 서비스는 포트가 `false` 를 주면 증가하지 않는다 → Task 2 `recordView_중복이면_증가하지_않고_현재값을_반환한다`.
2. **깨지거나 조작된 `visitorId` 쿠키(`not-a-uuid`, 비표준형 `1-1-1-1-1`)** — 500 이 아니라 새 쿠키를 발급하고 정상 처리해야 한다 → Task 4 `형식이_잘못된_쿠키면_새로_발급한다`.
3. **삭제된 글·없는 글에 조회 기록** — 404 이고 Redis 키를 만들지 않아야 한다 → Task 2 `recordView_삭제된_글이면_404_이고_중복판정을_하지_않는다`·`recordView_없는_글이면_404`.
4. **Redis 장애** — 조회 기록 API 가 500 을 내지 않고 세지 않아야 한다 → Task 3 `Redis_예외면_false_를_반환하고_전파하지_않는다`.
5. **로그인 사용자가 예전 `visitorId` 쿠키도 갖고 있음** — 회원 키로 세고 쿠키를 읽지도 재발급하지도 않아야 한다 → Task 5 `로그인이면_회원키로_기록하고_쿠키를_발급하지_않는다`.

---

## File Map

| 파일 | 책임 | Task |
|---|---|---|
| `module/common/src/main/java/com/example/lolserver/common/config/CookieProperties.java` (이동) | 쿠키 공통 설정(`cookie.*`) | 1 |
| `module/domain/community/src/main/java/com/example/lolserver/community/domain/vo/ViewerKey.java` | 방문자 식별 값 | 2 |
| `…/community/application/port/in/PostViewUseCase.java` | 조회 기록 유스케이스 | 2 |
| `…/community/application/port/out/PostViewDedupPort.java` | 중복 판정 포트 | 2 |
| `…/community/config/CommunityViewProperties.java` | `community.view.dedup-ttl` | 2 |
| `…/community/application/PostViewService.java` | 조회 기록 처리 | 2 |
| `…/community/adapter/out/cache/PostViewDedupRedisAdapter.java` | Redis `SET NX` | 3 |
| `…/community/adapter/in/web/VisitorCookieManager.java` | `visitorId` 읽기·발급 | 4 |
| `…/community/adapter/in/web/CommunityPostViewController.java` | 조회 기록 API | 5 |
| `…/community/adapter/in/web/response/PostViewResponse.java` | 응답 DTO | 5 |
| `module/domain/member/src/main/java/com/example/lolserver/member/config/SecurityConfig.java` | 비로그인 허용 규칙 | 5 |
| `…/community/application/PostService.java`, `PostDetailReadModel.java`, `PostResponse.java` | 상세 GET 정리·`viewerAuthenticated` | 6 |

`…/community/` = `module/domain/community/src/main/java/com/example/lolserver/community/`, 테스트는 같은 경로의 `src/test/java`.

---

### Task 1: `CookieProperties` 를 common 으로 이동

동작 변화 없는 이동이다. community 가 member 내부(`member.config`)에 의존할 수 없어서 공용 위치로 옮긴다.

**Files:**
- Move: `module/domain/member/src/main/java/com/example/lolserver/member/config/CookieProperties.java` → `module/common/src/main/java/com/example/lolserver/common/config/CookieProperties.java` (패키지 `com.example.lolserver.common.config`, 내용 그대로)
- Modify (import 변경): `module/domain/member/src/main/java/com/example/lolserver/member/adapter/in/web/security/AuthCookieManager.java:3`
- Modify (import 변경): `module/domain/member/src/test/java/com/example/lolserver/member/adapter/in/web/security/AuthCookieManagerTest.java:3`, `…/security/SecurityConfigTest.java:6`
- Modify (import 추가 — 지금은 같은 패키지라 import 가 없다): `module/domain/member/src/test/java/com/example/lolserver/member/config/SecurityConfigAuthorizationTest.java`

**Interfaces:**
- Produces: `com.example.lolserver.common.config.CookieProperties` — `boolean isSecure()`, `String getSameSite()`, `String getDomain()` (prefix `cookie`, `@Component`)

- [ ] **Step 1: `git mv` 로 파일을 옮기고 package 선언을 `com.example.lolserver.common.config` 로 바꾼다**
- [ ] **Step 2: 위 4개 파일의 import 를 `com.example.lolserver.common.config.CookieProperties` 로 맞춘다**
- [ ] **Step 3: 검증**

Run: `./gradlew compileJava compileTestJava && ./gradlew :module:domain:member:test`
Expected: BUILD SUCCESSFUL (기존 테스트 전부 통과)

- [ ] **Step 4: Commit**

```bash
git add -A module/common module/domain/member
git commit -m "refactor: 쿠키 설정을 common 으로 옮겨 커뮤니티와 공유한다"
```

---

### Task 2: 조회 기록 유스케이스

**Files:**
- Create: `…/community/domain/vo/ViewerKey.java`
- Create: `…/community/application/port/in/PostViewUseCase.java`
- Create: `…/community/application/port/out/PostViewDedupPort.java`
- Create: `…/community/config/CommunityViewProperties.java`
- Create: `…/community/application/PostViewService.java`
- Test: `module/domain/community/src/test/java/com/example/lolserver/community/domain/ViewerKeyTest.java`
- Test: `module/domain/community/src/test/java/com/example/lolserver/community/application/PostViewServiceTest.java`

**Interfaces:**
- Consumes: `PostPersistencePort.findById(Long): Optional<Post>`, `PostPersistencePort.incrementViewCount(Long)`, `Post.validateNotDeleted()`, `Post.incrementViewCount()`, `Post.getViewCount(): int`
- Produces:
  - `public record ViewerKey(String value)` + `static ViewerKey ofMember(Long memberId)`, `static ViewerKey ofVisitor(UUID visitorId)` (null 인자는 `NullPointerException`)
  - `public interface PostViewUseCase { int recordView(Long postId, ViewerKey viewer); }` — 반환값은 기록 후 화면에 보일 조회수
  - `public interface PostViewDedupPort { boolean tryMarkViewed(Long postId, ViewerKey viewer, Duration ttl); }` — 창 안의 첫 조회일 때만 `true`. 중복·저장소 장애는 `false`
  - `CommunityViewProperties` (`@Getter @Setter @Component @ConfigurationProperties(prefix = "community.view")`, `Duration dedupTtl = Duration.ofHours(24)`) — `CommunityImageProperties` 와 같은 형식

- [ ] **Step 1: 실패하는 테스트 작성**

```java
// ViewerKeyTest
@Test void 회원키는_m_접두사() { assertThat(ViewerKey.ofMember(7L).value()).isEqualTo("m:7"); }
@Test void 방문자키는_v_접두사() {
    UUID id = UUID.fromString("0b7c6d7e-1f2a-4b3c-8d4e-5f6a7b8c9d0e");
    assertThat(ViewerKey.ofVisitor(id).value()).isEqualTo("v:0b7c6d7e-1f2a-4b3c-8d4e-5f6a7b8c9d0e");
}

// PostViewServiceTest — @ExtendWith(MockitoExtension.class), @Mock PostPersistencePort, @Mock PostViewDedupPort,
// @BeforeEach: service = new PostViewService(postPersistencePort, postViewDedupPort, new CommunityViewProperties())
// fixture: Post.builder().id(1L).memberId(1L).viewCount(10).deleted(false).build()
@Test void recordView_첫조회면_증가하고_증가값을_반환한다() {
    // given findById(1L)=post, tryMarkViewed(1L, ViewerKey.ofMember(7L), Duration.ofHours(24))=true
    assertThat(service.recordView(1L, ViewerKey.ofMember(7L))).isEqualTo(11);
    then(postPersistencePort).should().incrementViewCount(1L);
}
@Test void recordView_중복이면_증가하지_않고_현재값을_반환한다() {
    // tryMarkViewed(...)=false
    assertThat(service.recordView(1L, ViewerKey.ofMember(7L))).isEqualTo(10);
    then(postPersistencePort).should(never()).incrementViewCount(any());
}
@Test void recordView_없는_글이면_404() {
    // findById(999L)=Optional.empty()
    assertThatThrownBy(() -> service.recordView(999L, ViewerKey.ofMember(7L)))
            .isInstanceOf(CoreException.class)
            .extracting(e -> ((CoreException) e).getErrorType()).isEqualTo(ErrorType.POST_NOT_FOUND);
    then(postViewDedupPort).shouldHaveNoInteractions();
}
@Test void recordView_삭제된_글이면_404_이고_중복판정을_하지_않는다() {
    // fixture 를 deleted(true) 로
    // 위와 같은 POST_NOT_FOUND 단언 + then(postViewDedupPort).shouldHaveNoInteractions();
}
```

- [ ] **Step 2: 실패 확인**

Run: `./gradlew :module:domain:community:test --tests "*ViewerKeyTest" --tests "*PostViewServiceTest"`
Expected: FAIL — `ViewerKey`·`PostViewService` 등 심볼 없음(컴파일 오류)

- [ ] **Step 3: 위 Interfaces 의 타입들과 `PostViewService` 구현**

`PostViewService implements PostViewUseCase` — `@Service @RequiredArgsConstructor @Transactional`, 의존 `PostPersistencePort`·`PostViewDedupPort`·`CommunityViewProperties`. 순서: `findById` → 없으면 `CoreException(POST_NOT_FOUND)` → `validateNotDeleted()` → `tryMarkViewed(postId, viewer, properties.getDedupTtl())` → `true` 면 `incrementViewCount(postId)` + `post.incrementViewCount()` → `post.getViewCount()` 반환.

- [ ] **Step 4: 통과 확인**

Run: `./gradlew :module:domain:community:test --tests "*ViewerKeyTest" --tests "*PostViewServiceTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add module/domain/community
git commit -m "feat: 게시글 조회 기록 유스케이스와 중복 판정 포트를 추가한다"
```

---

### Task 3: Redis 중복 판정 어댑터

**Files:**
- Create: `…/community/adapter/out/cache/PostViewDedupRedisAdapter.java`
- Test: `module/domain/community/src/test/java/com/example/lolserver/community/adapter/out/cache/PostViewDedupRedisAdapterTest.java`

**Interfaces:**
- Consumes: `PostViewDedupPort` (Task 2), `StringRedisTemplate`(Spring Boot 자동 구성 빈 — 같은 모듈 `RedisImageRateLimitAdapter` 가 이미 주입받아 쓴다)
- Produces: `@Component PostViewDedupRedisAdapter implements PostViewDedupPort`

- [ ] **Step 1: 실패하는 테스트 작성** — `@Mock StringRedisTemplate`, `@Mock ValueOperations<String, String>`, `given(template.opsForValue()).willReturn(ops)`

```java
private static final String KEY = "community:post-view:42:m:7";
private static final Duration TTL = Duration.ofHours(24);

@Test void 처음이면_true() {
    given(ops.setIfAbsent(KEY, "1", TTL)).willReturn(Boolean.TRUE);
    assertThat(adapter.tryMarkViewed(42L, ViewerKey.ofMember(7L), TTL)).isTrue();
}
@Test void 이미_있으면_false() { /* setIfAbsent → Boolean.FALSE */ assertThat(...).isFalse(); }
@Test void 결과가_null_이면_false() { /* setIfAbsent → null */ assertThat(...).isFalse(); }
@Test void Redis_예외면_false_를_반환하고_전파하지_않는다() {
    given(ops.setIfAbsent(KEY, "1", TTL)).willThrow(new RedisConnectionFailureException("down"));
    assertThat(adapter.tryMarkViewed(42L, ViewerKey.ofMember(7L), TTL)).isFalse();
}
```

- [ ] **Step 2: 실패 확인**

Run: `./gradlew :module:domain:community:test --tests "*PostViewDedupRedisAdapterTest"`
Expected: FAIL — `PostViewDedupRedisAdapter` 없음

- [ ] **Step 3: 구현** — 키 `"community:post-view:" + postId + ":" + viewer.value()`, `opsForValue().setIfAbsent(key, "1", ttl)` 결과를 `Boolean.TRUE.equals(...)` 로. `RuntimeException` 은 잡아 `log.warn`(postId·viewer 포함) 후 `false`. 예외 처리 방식은 `RedisImageRateLimitAdapter` 와 같되, 이쪽은 장애 시 **세지 않는** 쪽(spec 5.1)이라는 이유를 주석으로 남긴다.

- [ ] **Step 4: 통과 확인** — Step 2 명령, Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add module/domain/community
git commit -m "feat: Redis SET NX 로 게시글 조회 중복을 판정한다"
```

---

### Task 4: 방문자 쿠키 관리

**Files:**
- Create: `…/community/adapter/in/web/VisitorCookieManager.java`
- Test: `module/domain/community/src/test/java/com/example/lolserver/community/adapter/in/web/VisitorCookieManagerTest.java`

**Interfaces:**
- Consumes: `CookieProperties` (Task 1)
- Produces: `@Component VisitorCookieManager` — `UUID resolveOrIssue(HttpServletRequest request, HttpServletResponse response)`. 상수 `static final String COOKIE_NAME = "visitorId"`.

- [ ] **Step 1: 실패하는 테스트 작성** — `MockHttpServletRequest`/`MockHttpServletResponse`, `CookieProperties` 는 `secure=true`, `sameSite="None"`, `domain=""` 로 생성

```java
@Test void 쿠키가_없으면_새로_발급한다() {
    UUID id = manager.resolveOrIssue(request, response);
    String setCookie = response.getHeader(HttpHeaders.SET_COOKIE);
    assertThat(setCookie).contains("visitorId=" + id, "Path=/api", "Max-Age=31536000", "HttpOnly", "Secure", "SameSite=None");
}
@Test void 유효한_쿠키면_그_값을_쓰고_재발급하지_않는다() {
    request.setCookies(new Cookie("visitorId", "0b7c6d7e-1f2a-4b3c-8d4e-5f6a7b8c9d0e"));
    assertThat(manager.resolveOrIssue(request, response)).isEqualTo(UUID.fromString("0b7c6d7e-1f2a-4b3c-8d4e-5f6a7b8c9d0e"));
    assertThat(response.getHeader(HttpHeaders.SET_COOKIE)).isNull();
}
@ParameterizedTest @ValueSource(strings = {"not-a-uuid", "1-1-1-1-1", ""})
void 형식이_잘못된_쿠키면_새로_발급한다(String raw) {
    request.setCookies(new Cookie("visitorId", raw));
    UUID id = manager.resolveOrIssue(request, response);
    assertThat(id.toString()).isNotEqualTo(raw);
    assertThat(response.getHeader(HttpHeaders.SET_COOKIE)).contains("visitorId=" + id);
}
@Test void domain_설정이_있으면_Domain_을_싣는다() {
    // domain="metapick.me" 로 만든 manager → Set-Cookie 에 "Domain=metapick.me"
}
```

- [ ] **Step 2: 실패 확인**

Run: `./gradlew :module:domain:community:test --tests "*VisitorCookieManagerTest"`
Expected: FAIL — `VisitorCookieManager` 없음

- [ ] **Step 3: 구현** — 쿠키 값은 `UUID.fromString(raw)` 가 성공하고 `parsed.toString().equals(raw)` 일 때만 인정한다(비표준형 거부). 아니면 `UUID.randomUUID()` 를 `ResponseCookie` 로 발급: `httpOnly(true)`, `secure(cookieProperties.isSecure())`, `sameSite(cookieProperties.getSameSite())`, `path("/api")`, `maxAge(Duration.ofDays(365))`, domain 이 비어 있지 않을 때만 `domain(...)`. 발급 형식은 member 의 `AuthCookieManager.addCookie` 와 같다.

- [ ] **Step 4: 통과 확인** — Step 2 명령, Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add module/domain/community
git commit -m "feat: 비로그인 방문자를 식별하는 visitorId 쿠키를 발급한다"
```

---

### Task 5: 조회 기록 API · 보안 규칙 · RestDocs

**Files:**
- Create: `…/community/adapter/in/web/response/PostViewResponse.java`
- Create: `…/community/adapter/in/web/CommunityPostViewController.java`
- Modify: `module/domain/member/src/main/java/com/example/lolserver/member/config/SecurityConfig.java:126` (커뮤니티 `authenticated()` 규칙 **바로 앞**)
- Modify: `module/domain/member/src/test/java/com/example/lolserver/member/config/SecurityConfigAuthorizationTest.java` (`비로그인_공개`·`비로그인_401` 의 `@CsvSource`)
- Create: `module/app/application/src/docs/asciidoc/api/community/community-post-view.adoc`
- Modify: `module/app/application/src/docs/asciidoc/index.adoc:143` 다음 줄(빈 줄 하나 두고 `include::api/community/community-post-view.adoc[]`)
- Test: `module/domain/community/src/test/java/com/example/lolserver/community/adapter/in/web/CommunityPostViewControllerTest.java`

**Interfaces:**
- Consumes: `PostViewUseCase.recordView(Long, ViewerKey): int` (Task 2), `VisitorCookieManager.resolveOrIssue(...)` (Task 4), `AuthenticatedMember.memberId()`
- Produces:
  - `public record PostViewResponse(int viewCount)`
  - `CommunityPostViewController` — `@RestController @RequestMapping("/api/v1/community")`(v1 만), `@PostMapping("/posts/{postId}/views") ResponseEntity<ApiResponse<PostViewResponse>> recordView(@PathVariable Long postId, @AuthenticationPrincipal AuthenticatedMember member, HttpServletRequest request, HttpServletResponse response)`. 회원이면 `ViewerKey.ofMember(member.memberId())`(쿠키 미접근), 아니면 `ViewerKey.ofVisitor(visitorCookieManager.resolveOrIssue(request, response))`. 응답 `ResponseEntity.ok(ApiResponse.success(new PostViewResponse(count)))`.

> spec 5.1 은 기존 컨트롤러의 두 경로 매핑을 전제했지만, 별도 컨트롤러로 두면서 v1 만 매핑한다. 옛 경로로 부를 클라이언트가 없다는 같은 이유다.

- [ ] **Step 1: 보안 테스트에 기대값 추가** — `비로그인_공개` 에 `"POST, /api/v1/community/posts/1/views"`, `비로그인_401` 에 `"POST, /api/community/posts/1/views"`
- [ ] **Step 2: 실패 확인**

Run: `./gradlew :module:domain:member:test --tests "*SecurityConfigAuthorizationTest"`
Expected: FAIL — `POST /api/v1/community/posts/1/views` 가 401

- [ ] **Step 3: `SecurityConfig` 에 규칙 추가**

```java
// 조회 기록은 비로그인 방문자도 남긴다.
.requestMatchers(HttpMethod.POST, "/api/v1/community/posts/*/views").permitAll()
```

- [ ] **Step 4: 통과 확인** — Step 2 명령, Expected: PASS
- [ ] **Step 5: 컨트롤러 테스트 작성** — `CommunityPostViewControllerTest extends RestDocsSupport`, `@ExtendWith(MockitoExtension.class)`, `@Mock PostViewUseCase`. `initController()` 는 실제 `VisitorCookieManager(new CookieProperties())` 를 넣어 만든다. `customArgumentResolvers()` 는 **요청 시점에** 테스트 필드 `member`(기본 `null` = 비로그인)를 돌려주는 `AuthenticatedMember` 리졸버를 반환한다(`RestDocsSupport` 의 setUp 이 테스트 메서드보다 먼저 돌기 때문). 경로 변수 문서화를 위해 `RestDocumentationRequestBuilders.post("/api/v1/community/posts/{postId}/views", 42L)` 를 쓴다.

```java
@Test void 비로그인_쿠키가_없으면_방문자키로_기록하고_쿠키를_발급한다() {
    given(postViewUseCase.recordView(eq(42L), any())).willReturn(124);
    mockMvc.perform(post("/api/v1/community/posts/{postId}/views", 42L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.viewCount").value(124))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("visitorId=")))
            .andDo(document("community-post-view",
                    pathParameters(parameterWithName("postId").description("게시글 ID")),
                    responseHeaders(headerWithName(HttpHeaders.SET_COOKIE).optional()
                            .description("비로그인이고 visitorId 쿠키가 없거나 잘못됐을 때만 발급")),
                    responseFields(/* result, errorMessage(NULL), data.viewCount(NUMBER: 이번 기록 반영 후 조회수) */)));
    ArgumentCaptor<ViewerKey> captor = ArgumentCaptor.forClass(ViewerKey.class);
    verify(postViewUseCase).recordView(eq(42L), captor.capture());
    assertThat(captor.getValue().value()).startsWith("v:");
}
@Test void 비로그인_쿠키가_있으면_그_값으로_기록하고_재발급하지_않는다() {
    // .cookie(new Cookie("visitorId", "0b7c6d7e-1f2a-4b3c-8d4e-5f6a7b8c9d0e"))
    // captor 값 == "v:0b7c6d7e-1f2a-4b3c-8d4e-5f6a7b8c9d0e", header().doesNotExist(SET_COOKIE)
}
@Test void 로그인이면_회원키로_기록하고_쿠키를_발급하지_않는다() {
    // member = new AuthenticatedMember(7L, "USER"); visitorId 쿠키도 함께 보낸다
    // captor 값 == "m:7", header().doesNotExist(SET_COOKIE)
}
```

- [ ] **Step 6: 실패 확인**

Run: `./gradlew :module:domain:community:test --tests "*CommunityPostViewControllerTest"`
Expected: FAIL — `CommunityPostViewController` 없음

- [ ] **Step 7: `PostViewResponse`·`CommunityPostViewController` 구현**
- [ ] **Step 8: 통과 확인** — Step 6 명령, Expected: PASS
- [ ] **Step 9: adoc 추가** — `community-post-view.adoc` 를 `community-post-detail.adoc` 형식으로 작성(제목 `=== 게시글 조회 기록`, 앵커 `[[community-post-view]]`, 스니펫 `http-request`·`path-parameters`·`http-response`·`response-headers`·`response-fields`). 본문에 "같은 방문자·같은 글은 첫 기록 후 24시간 동안 1회만 센다"를 한 줄로 적는다. `index.adoc` 에 include.
- [ ] **Step 10: Commit**

```bash
git add module/domain/community module/domain/member module/app/application/src/docs
git commit -m "feat: 게시글 조회 기록 API 를 추가하고 비로그인 호출을 허용한다"
```

---

### Task 6: 상세 GET 정리와 `viewerAuthenticated`

**Files:**
- Modify: `…/community/application/PostService.java:122-148` (`getPost`)
- Modify: `…/community/application/model/readmodel/PostDetailReadModel.java`
- Modify: `…/community/adapter/in/web/response/PostResponse.java`
- Modify: `module/domain/community/src/test/java/com/example/lolserver/community/application/PostServiceTest.java:259-278` (`getPost_success`)
- Test (Create): `module/domain/community/src/test/java/com/example/lolserver/community/adapter/in/web/CommunityPostControllerTest.java` — 상세 조회 RestDocs(`community-post-detail` 스니펫. 기존 `community-post-detail.adoc` 가 include 하지만 만드는 테스트가 없었다)

**Interfaces:**
- Produces:
  - `PostDetailReadModel` 필드 `private final boolean viewerAuthenticated;`(`currentUserBookmarked` 다음). 팩토리 `of(Post, MemberProfileReadModel, Vote, boolean currentUserBookmarked, List<PostImageReadModel> images, boolean viewerAuthenticated)` — 기존 5인자 `of` 를 이 6인자로 바꾸고, 4인자 `of` 는 `(…, List.of(), false)` 로 위임
  - `PostResponse` 컴포넌트 `boolean viewerAuthenticated` (`currentUserBookmarked` 다음). `from(PostDetailReadModel)` 은 `readModel.isViewerAuthenticated()`, `from(PostDetailResultModel)` 은 `true`

- [ ] **Step 1: `PostServiceTest` 수정·추가**

```java
@DisplayName("게시글 상세 조회는 조회수를 바꾸지 않고 상세 정보를 반환한다")
@Test void getPost_success() {
    // 기존 given 유지, postService.getPost(postId, null)
    assertThat(result.getId()).isEqualTo(postId);
    assertThat(result.isViewerAuthenticated()).isFalse();
    then(postPersistencePort).should(never()).incrementViewCount(any());
}
@Test void getPost_로그인이면_viewerAuthenticated_true() {
    // given findById/getMemberProfile 스텁, postService.getPost(postId, 2L)
    assertThat(result.isViewerAuthenticated()).isTrue();
}
```

- [ ] **Step 2: 실패 확인**

Run: `./gradlew :module:domain:community:test --tests "*PostServiceTest"`
Expected: FAIL — `isViewerAuthenticated` 없음

- [ ] **Step 3: 구현** — `getPost` 에서 `incrementViewCount` 두 줄(`PostService.java:130-131`)과 메서드의 `@Transactional` 을 지운다(클래스 기본 `readOnly = true` 적용). 6인자 `of(..., currentMemberId != null)` 로 호출. `PostDetailReadModel`·`PostResponse` 는 위 Interfaces 대로.

- [ ] **Step 4: 통과 확인** — Step 2 명령, Expected: PASS
- [ ] **Step 5: 상세 조회 RestDocs 테스트 작성** — `CommunityPostControllerTest extends RestDocsSupport`, `@Mock PostUseCase`, `@Mock PostQueryUseCase`, `initController()` = `new CommunityPostController(postUseCase, postQueryUseCase)`, 리졸버 `new TestAuthenticatedMemberResolver()`(memberId 1). `postQueryUseCase.getPost(1L, 1L)` 가 `viewerAuthenticated=true`, `currentUserVote=VoteType.UPVOTE`, `images=List.of()`, 작성자 프로필이 채워진 `PostDetailReadModel` 을 반환하게 builder 로 만든다.

```java
@Test void getPost_상세조회_문서화() {
    mockMvc.perform(RestDocumentationRequestBuilders.get("/api/v1/community/posts/{postId}", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.viewerAuthenticated").value(true))
            .andDo(document("community-post-detail",
                    pathParameters(parameterWithName("postId").description("게시글 ID")),
                    responseFields(/* PostResponse 의 모든 필드:
                        id, title, content, categoryId, viewCount, upvoteCount, downvoteCount, commentCount,
                        author.id, author.nickname, author.profileImageUrl,
                        currentUserVote(STRING, optional), currentUserBookmarked(BOOLEAN),
                        viewerAuthenticated(BOOLEAN: "서버가 이 요청을 로그인 회원으로 인식했는가. false 이면 개인화 필드가 비어 있다"),
                        images[](ARRAY), createdAt(STRING), updatedAt(STRING), 그리고 result·errorMessage */)));
}
```

- [ ] **Step 6: 통과 확인**

Run: `./gradlew :module:domain:community:test --tests "*CommunityPostControllerTest" --tests "*PostServiceTest"`
Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add module/domain/community
git commit -m "feat: 상세 조회는 조회수를 올리지 않고 viewerAuthenticated 를 싣는다"
```

---

### Task 7: 전체 검증과 설계 문서 동기화

**Files:**
- Modify: `docs/community-view-dedup.md` — 상단 상태를 `**구현 완료(lol-ui 대기)**` 로, 5.1 에 "별도 컨트롤러·v1 만 매핑", 5.1 의 Redis 장애 처리 위치를 "어댑터가 잡아 `false`" 로 구현 노트 추가

- [ ] **Step 1: 전 모듈 컴파일** — Run: `./gradlew compileJava compileTestJava`, Expected: BUILD SUCCESSFUL
- [ ] **Step 2: 아키텍처 규칙** — Run: `./gradlew archTest`, Expected: BUILD SUCCESSFUL
- [ ] **Step 3: 영향 모듈 테스트** — Run: `./gradlew :module:domain:community:test :module:domain:member:test`, Expected: BUILD SUCCESSFUL
- [ ] **Step 4: RestDocs HTML** — `./gradlew :module:app:application:tasks --all | grep -i asciidoctor` 로 태스크 이름을 확인해 실행하고, 생성된 HTML 에 "게시글 조회 기록"·`viewerAuthenticated` 가 보이는지 확인한다(CLAUDE.md 의 `:module:infra:api:asciidoctor` 는 옛 구조라 쓰지 않는다)
- [ ] **Step 5: 설계 문서 갱신 후 Commit**

```bash
git add docs/community-view-dedup.md
git commit -m "docs: 조회수 중복 방지 설계에 구현 노트를 반영한다"
```

배포: 이 계획의 결과를 먼저 배포하고 lol-ui 를 곧바로 배포한다(spec 5.7). DB 마이그레이션·Redis flush 없음.
