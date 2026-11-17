# 커뮤니티 조회수 중복 방지 — lol-ui 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 앱 진입 시 로그인 세션을 먼저 확정하고, 게시글 상세는 SSR 에서 로그인 토큰을 실어 개인화한 뒤, 브라우저가 조회 기록 API 를 한 번만 부르게 한다.

**Architecture:** 인증 스토어에 `status` 를 두고 `SessionBootstrap` 이 전체 페이지 로드당 한 번 `/v1/members/me` 로 세션을 검사한다(401→refresh 는 기존 인터셉터, 실패해도 로그인 화면으로 보내지 않음). 상세 SSR 은 `accessToken` 쿠키를 `Authorization` 헤더로 전달하고, 상세 화면은 `status` 가 확정된 뒤 조회 기록을 보내며 서버가 비로그인으로 응답한 경우에만 재조회한다.

**Tech Stack:** Next.js 16 App Router, React 19, TanStack Query 5, Zustand(persist), axios, Vitest(jsdom).

**Spec:** lol-server `docs/community-view-dedup.md` (브랜치 `docs/community-view-dedup-design`). 백엔드 계획: lol-server `docs/superpowers/plans/2026-11-18-community-view-dedup-backend.md`.

## Global Constraints

- 작업 위치: `/Users/sanhgoon/Desktop/lol/lol-ui`, `origin/develop` 기준 브랜치(예: `feature/community-view-dedup`). 브랜치·커밋·PR 제목에 `MP-*` 를 넣지 않는다.
- 커밋 메시지: `<type>: <한글 설명>`.
- FSD 의존 방향 `app → views → widgets → features → entities → shared`. `shared` 는 `entities` 를 import 하지 않는다.
- 단위 테스트는 `src/**/*.test.ts` 만 실행된다(Vitest, jsdom). `.tsx` 테스트·Testing Library 는 없다 → 판단 로직은 순수 함수로 빼서 테스트하고, 컴포넌트 연결은 타입체크·빌드·수동 확인으로 검증한다.
- API 경로는 `apiClient` baseURL(`…/api`) 기준 `/v1/...`. 조회 기록: `POST /v1/community/posts/{postId}/views` → `ApiResponse<{ viewCount: number }>`. 세션 검사: `GET /v1/members/me`.
- 액세스 토큰 쿠키 이름 `accessToken`.
- 세션 검사는 **저장된 `user` 가 있을 때만**, 전체 페이지 로드당 1회. refresh 실패 시 `/login` 으로 보내지 않는다(세션 검사 요청에 한해서. 다른 요청의 기존 동작은 그대로).
- 게시글 상세 쿼리는 invalidate 하지 않는다 — 바뀐 필드만 `setQueryData` 로 쓴다(기존 규칙 유지).
- 수정 화면(`CommunityEditPageClient`)은 조회 기록을 보내지 않는다.
- 배포는 lol-server 이후(spec 5.7). Task 1·2(세션 검사)는 백엔드와 무관해 먼저 나가도 된다.
- 검증 명령: `pnpm test`, `pnpm lint`, `npx tsc --noEmit`, `pnpm build`.

## Review Focus

1. **refresh token 까지 만료된 사용자가 글 링크로 들어옴** — 로그인 화면으로 튕기지 않고 비로그인으로 글을 계속 읽어야 한다 → Task 1 `skipAuthRedirect 요청은 refresh 가 실패해도 저장소를 지우지 않고 reject 한다`, Task 2 `401 이 두 번이면 조용히 로그아웃한다`.
2. **다른 탭이 먼저 refresh 해 이 탭의 refresh 가 실패** — 재확인으로 로그인이 유지돼야 한다 → Task 2 `401 뒤 재확인이 성공하면 authenticated`.
3. **오프라인·서버 장애 중 진입** — 로그아웃되지 않아야 한다 → Task 2 `네트워크 오류면 user 를 유지하고 unverified`.
4. **SSR 이 실패해 `initialPost` 없이 클라이언트가 상세를 받았는데 비로그인 응답** — 세션 확정 뒤 개인화 재조회가 일어나야 한다 → Task 5 테스트가 `initialPost` 가 아니라 현재 `post` 로 판단하는지 검증.
5. **조회 기록 API 가 404·500** — 화면이 깨지지 않고 조회수는 그대로여야 한다 → Task 4 `실패해도 throw 하지 않고 캐시를 유지한다`.

---

## File Map

| 파일 | 책임 | Task |
|---|---|---|
| `src/shared/api/client.ts` | `skipAuthRedirect` 옵션 | 1 |
| `src/entities/auth/types.ts`, `model/useAuthStore.ts`, `api/authApi.ts`, `index.ts` | `AuthStatus`·`status`, `getMyProfile(config)` | 2 |
| `src/features/auth/model/bootstrapSession.ts` | 세션 검사 판단(순수 함수) | 2 |
| `src/features/auth/ui/SessionBootstrap.tsx`, `src/features/auth/index.ts` | 전체 페이지 로드당 1회 실행 | 2 |
| `src/app/(main)/[locale]/layout.tsx` | `SessionBootstrap` 마운트 | 2 |
| `src/shared/api/authHeaders.ts` | `Authorization` 헤더 생성 | 3 |
| `src/entities/community/api/communityApi.ts` | `getPostDetail(config)`, `recordPostView` | 3, 4 |
| `src/views/community/lib/loadCommunityData.ts` | SSR 상세 조회에 토큰 전달 | 3 |
| `src/entities/community/types.ts` | `Post.viewerAuthenticated`, `PostViewResult` | 4 |
| `src/entities/community/model/recordPostView.ts`, `useRecordPostView.ts`, `index.ts` | 조회 기록 + 캐시 동기화 | 4 |
| `src/widgets/community-detail/lib/shouldRefetchPersonalization.ts` | 재조회 판단 | 5 |
| `src/widgets/community-detail/ui/PostDetailPanel.tsx` | 화면 연결 | 5 |

---

### Task 1: 인터셉터 `skipAuthRedirect`

**Files:**
- Modify: `src/shared/api/client.ts:4-10`(axios 타입 확장), `:142-150`(refresh 실패 catch)
- Test: `src/shared/api/client.test.ts` (기존 adapter 하네스 재사용)

**Interfaces:**
- Produces: axios 요청 옵션 `skipAuthRedirect?: boolean` — `declare module "axios"` 블록에 `interface AxiosRequestConfig { skipAuthRedirect?: boolean }` 를 추가해 `apiClient.get(url, { skipAuthRedirect: true })` 가 타입 검사를 통과하게 한다. 이 옵션이 켜진 요청은 refresh 가 실패하면 `clearAuthStorage()`·`/login` 이동 없이 refresh 오류로 reject 한다.

- [ ] **Step 1: 실패하는 테스트 작성** (`describe("skipAuthRedirect")`)

```ts
it("skipAuthRedirect 요청은 refresh 가 실패해도 저장소를 지우지 않고 reject 한다", async () => {
  localStorage.setItem("auth-storage", "kept");
  replies["/api/v1/members/me"] = [{ status: 401 }];
  replies["/api/v1/auth/refresh"] = [{ status: 401 }];
  replies["/api/auth/refresh"] = [{ status: 401 }];

  await expect(apiClient.get("/v1/members/me", { skipAuthRedirect: true }))
    .rejects.toMatchObject({ response: { status: 401 } });
  expect(localStorage.getItem("auth-storage")).toBe("kept");
});

it("skipAuthRedirect 가 없으면 refresh 실패 시 저장소를 지운다", async () => {
  // 위와 같은 replies, 옵션 없이 호출 → reject, localStorage.getItem("auth-storage") === null
});
```

- [ ] **Step 2: 실패 확인**

Run: `pnpm test src/shared/api/client.test.ts`
Expected: FAIL — 첫 테스트에서 `auth-storage` 가 `null`(그리고 `skipAuthRedirect` 타입 오류)

- [ ] **Step 3: 구현** — refresh 실패 `catch` 에서 `processQueue(refreshError)` 뒤 `originalRequest.skipAuthRedirect` 이면 바로 `return Promise.reject(refreshError)`. 그 밖의 경로는 그대로 둔다.

- [ ] **Step 4: 통과 확인** — Step 2 명령, Expected: PASS(기존 테스트 포함)

- [ ] **Step 5: Commit**

```bash
git add src/shared/api/client.ts src/shared/api/client.test.ts
git commit -m "feat: 요청별로 refresh 실패 시 로그인 이동을 끌 수 있게 한다"
```

---

### Task 2: 인증 상태와 앱 진입 세션 검사

**Files:**
- Modify: `src/entities/auth/types.ts`, `src/entities/auth/model/useAuthStore.ts`, `src/entities/auth/api/authApi.ts:9-12`, `src/entities/auth/index.ts`
- Create: `src/features/auth/model/bootstrapSession.ts`
- Create: `src/features/auth/ui/SessionBootstrap.tsx`
- Modify: `src/features/auth/index.ts` (export `SessionBootstrap`)
- Modify: `src/app/(main)/[locale]/layout.tsx:73` (`<GameDataLoader />` 바로 다음에 `<SessionBootstrap />`)
- Test: `src/entities/auth/model/useAuthStore.test.ts`, `src/features/auth/model/bootstrapSession.test.ts`

**Interfaces:**
- Consumes: `skipAuthRedirect` (Task 1)
- Produces:
  - `export type AuthStatus = "unknown" | "authenticated" | "anonymous" | "unverified";` — `unverified` = 저장된 user 는 있으나 네트워크·5xx 로 확인하지 못함
  - `AuthState` 에 `status: AuthStatus`(초기 `"unknown"`, **persist 제외** — `partialize` 는 지금처럼 `user` 만), `setStatus(status: AuthStatus): void`. `setUser` 는 `status` 를 `"authenticated"` 로, `clearAuth` 는 `"anonymous"` 로 함께 바꾼다(로그인·로그아웃·탈퇴·닉네임 변경 호출부는 수정 불필요)
  - `getMyProfile(config?: AxiosRequestConfig): Promise<MemberProfile>`
  - `export interface SessionBootstrapDeps { hasStoredUser: boolean; fetchProfile: () => Promise<MemberProfile>; setUser: (u: MemberProfile) => void; clearAuth: () => void; setStatus: (s: AuthStatus) => void; }`
  - `export async function bootstrapSession(deps: SessionBootstrapDeps): Promise<AuthStatus>`
  - `export function SessionBootstrap(): null`

`bootstrapSession` 판단표(spec 5.6.0):

| 상황 | 동작 | 반환 |
|---|---|---|
| `hasStoredUser == false` | `setStatus("anonymous")`, 요청 없음 | `anonymous` |
| `fetchProfile` 성공 | `setUser(profile)` | `authenticated` |
| 401 (`axios.isAxiosError(e) && e.response?.status === 401`) | `fetchProfile` **한 번 더**: 성공 → `setUser` / 401 → `clearAuth()` / 그 밖 → `setStatus("unverified")` | 각각 `authenticated` / `anonymous` / `unverified` |
| 401 이 아닌 오류 | 재시도 없이 `setStatus("unverified")` | `unverified` |

- [ ] **Step 1: 실패하는 테스트 작성**

```ts
// useAuthStore.test.ts — 각 테스트 전에 useAuthStore.setState({ user: null, status: "unknown" }), localStorage.clear()
it("setUser 는 authenticated, clearAuth 는 anonymous 로 바꾼다", () => { /* status 단언 */ });
it("status 는 저장소에 남기지 않는다", () => {
  useAuthStore.getState().setUser(profile);
  expect(JSON.parse(localStorage.getItem("auth-storage")!).state).toEqual({ user: profile });
});

// bootstrapSession.test.ts — deps 는 vi.fn() 들, unauthorized = new AxiosError("401", "ERR_BAD_REQUEST", undefined, null, { status: 401 } as AxiosResponse)
it("저장된 user 가 없으면 요청 없이 anonymous", async () => {
  expect(await bootstrapSession({ ...deps, hasStoredUser: false })).toBe("anonymous");
  expect(deps.fetchProfile).not.toHaveBeenCalled();
  expect(deps.setStatus).toHaveBeenCalledWith("anonymous");
});
it("me 가 성공하면 setUser 하고 authenticated", ...);               // setUser(profile) 1회
it("401 뒤 재확인이 성공하면 authenticated", ...);                  // fetchProfile 2회, clearAuth 미호출
it("401 이 두 번이면 조용히 로그아웃한다", ...);                     // clearAuth 1회, 반환 anonymous
it("네트워크 오류면 user 를 유지하고 unverified", ...);              // new AxiosError("Network Error", "ERR_NETWORK"); fetchProfile 1회, clearAuth·setUser 미호출
it("500 이면 unverified", ...);
```

- [ ] **Step 2: 실패 확인**

Run: `pnpm test src/entities/auth src/features/auth`
Expected: FAIL — `status`·`bootstrapSession` 없음

- [ ] **Step 3: 스토어·타입·`getMyProfile(config)`·`bootstrapSession` 구현** (위 Interfaces·판단표대로)
- [ ] **Step 4: 통과 확인** — Step 2 명령, Expected: PASS
- [ ] **Step 5: `SessionBootstrap` 구현과 마운트** — `"use client"`. 모듈 스코프 `let started = false` 로 StrictMode·재마운트에도 1회만 돈다. effect 안에서 `useAuthStore.getState()` 를 읽어 `bootstrapSession({ hasStoredUser: state.user !== null, fetchProfile: () => getMyProfile({ skipAuthRedirect: true }), setUser, clearAuth, setStatus })` 를 호출한다(localStorage persist 는 동기 복원이라 effect 시점엔 이미 복원돼 있다). 렌더링은 `null`. `(main)/[locale]/layout.tsx` 에 마운트.
- [ ] **Step 6: 타입·린트** — Run: `npx tsc --noEmit && pnpm lint`, Expected: 오류 없음
- [ ] **Step 7: 수동 확인** — `pnpm dev`, 로그인 → DevTools 에서 `accessToken` 쿠키 삭제 → 새로고침. Network 에 `GET /v1/members/me` 401 → `POST /v1/auth/refresh` → `GET /v1/members/me` 200 순서, 헤더는 로그인 상태 유지. 이어서 `refreshToken` 쿠키까지 지우고 새로고침 → `/login` 으로 이동하지 않고 비로그인 헤더로 바뀜.
- [ ] **Step 8: Commit**

```bash
git add src/entities/auth src/features/auth "src/app/(main)/[locale]/layout.tsx"
git commit -m "feat: 앱 진입 시 로그인 세션을 먼저 확인하고 만료 토큰을 복구한다"
```

---

### Task 3: SSR 상세 조회에 `accessToken` 전달

**Files:**
- Create: `src/shared/api/authHeaders.ts`
- Test: `src/shared/api/authHeaders.test.ts`
- Modify: `src/entities/community/api/communityApi.ts:28-36` (`getPostDetail`)
- Modify: `src/views/community/lib/loadCommunityData.ts:35-37` (`loadPostDetail`)

**Interfaces:**
- Produces:
  - `export const ACCESS_TOKEN_COOKIE = "accessToken";`
  - `export function bearerAuthHeaders(token: string | undefined): Record<string, string>` — 토큰이 비면 `{}`, 아니면 `{ Authorization: \`Bearer ${token}\` }`
  - `getPostDetail(postId: number, client: AxiosInstance = apiClient, config?: AxiosRequestConfig): Promise<Post>` — `config` 를 `client.get` 에 그대로 넘긴다
  - `loadPostDetail` 은 `cache(async (postId: number) => …)` 를 유지하고, 안에서 `(await cookies()).get(ACCESS_TOKEN_COOKIE)?.value` 로 토큰을 읽어 `getPostDetail(postId, serverApiClient, { headers: bearerAuthHeaders(token) })` 를 호출한다(`cookies` 는 `next/headers`). 상세 조회에만 적용하고 `serverApiClient` 의 로그에 헤더를 추가하지 않는다.

- [ ] **Step 1: 실패하는 테스트 작성**

```ts
it("토큰이 없으면 빈 헤더", () => { expect(bearerAuthHeaders(undefined)).toEqual({}); expect(bearerAuthHeaders("")).toEqual({}); });
it("토큰이 있으면 Bearer 헤더", () => { expect(bearerAuthHeaders("abc")).toEqual({ Authorization: "Bearer abc" }); });
```

- [ ] **Step 2: 실패 확인** — Run: `pnpm test src/shared/api/authHeaders.test.ts`, Expected: FAIL — 모듈 없음
- [ ] **Step 3: `authHeaders.ts` 구현, `getPostDetail`·`loadPostDetail` 수정**
- [ ] **Step 4: 통과·타입 확인** — Run: `pnpm test src/shared/api && npx tsc --noEmit`, Expected: PASS, 오류 없음
- [ ] **Step 5: 수동 확인** — 로그인 후 내가 추천한 글의 상세 URL 로 직접 접속. 페이지 소스(SSR HTML)의 초기 데이터에 `currentUserVote` 가 채워져 있고, 추천 버튼이 첫 화면부터 켜져 있다(백엔드 배포 전이라도 확인 가능).
- [ ] **Step 6: Commit**

```bash
git add src/shared/api/authHeaders.ts src/shared/api/authHeaders.test.ts src/entities/community/api/communityApi.ts src/views/community/lib/loadCommunityData.ts
git commit -m "feat: 게시글 상세 SSR 조회에 로그인 토큰을 실어 개인화한다"
```

---

### Task 4: 조회 기록 API 연동

**Files:**
- Modify: `src/entities/community/types.ts` (`Post` 에 `viewerAuthenticated`, `PostViewResult` 추가)
- Modify: `src/entities/community/api/communityApi.ts` (`recordPostView` 추가)
- Create: `src/entities/community/model/recordPostView.ts`
- Create: `src/entities/community/model/useRecordPostView.ts`
- Modify: `src/entities/community/index.ts` (export `recordPostView`, `useRecordPostView`, `PostViewResult`)
- Test: `src/entities/community/model/recordPostView.test.ts`

**Interfaces:**
- Consumes: `postDetailKey(postId)` (`model/usePostDetail.ts`)
- Produces:
  - `Post.viewerAuthenticated: boolean` — 주석: "서버가 이 응답을 만들 때 로그인 회원으로 인식했는가. false 이면 `currentUserVote`·`currentUserBookmarked` 가 비어 있다"
  - `export interface PostViewResult { viewCount: number }`
  - `recordPostView(postId: number): Promise<PostViewResult>` — `apiClient.post<ApiResponse<PostViewResult>>(\`/v1/community/posts/${postId}/views\`)` 의 `data.data`
  - `recordPostViewAndSync(queryClient: QueryClient, postId: number): Promise<void>` — 성공하면 `setQueryData<Post>(postDetailKey(postId), prev => prev && { ...prev, viewCount })`, 실패하면 `logger.warn` 만 하고 resolve(throw 하지 않음)
  - `useRecordPostView(postId: number, enabled: boolean): void` — `useQueryClient()` + `useRef<number | null>`; `enabled` 이고 ref 가 `postId` 와 다를 때만 ref 를 갱신하고 `void recordPostViewAndSync(queryClient, postId)`

- [ ] **Step 1: 실패하는 테스트 작성** — `client.test.ts` 와 같은 방식으로 `apiClient.defaults.adapter` 를 가로채고 `new QueryClient()` 를 쓴다. fixture 는 `{ id: 42, viewCount: 10, viewerAuthenticated: false } as Post`.

```ts
it("성공하면 조회 기록 경로로 POST 하고 캐시의 viewCount 를 바꾼다", async () => {
  queryClient.setQueryData(postDetailKey(42), post);          // adapter 응답 data: { viewCount: 11 }
  await recordPostViewAndSync(queryClient, 42);
  expect(calls).toEqual(["POST /api/v1/community/posts/42/views"]);
  expect(queryClient.getQueryData<Post>(postDetailKey(42))?.viewCount).toBe(11);
});
it("실패해도 throw 하지 않고 캐시를 유지한다", async () => {
  // adapter 가 500 → await expect(recordPostViewAndSync(queryClient, 42)).resolves.toBeUndefined(); viewCount 10 유지
});
it("캐시가 비어 있으면 그대로 둔다", async () => {
  // setQueryData 없이 호출 → getQueryData(postDetailKey(42)) === undefined
});
```

- [ ] **Step 2: 실패 확인** — Run: `pnpm test src/entities/community/model/recordPostView.test.ts`, Expected: FAIL — 모듈 없음
- [ ] **Step 3: 타입·`recordPostView`·`recordPostViewAndSync`·`useRecordPostView` 구현**
- [ ] **Step 4: 통과·타입 확인** — Run: `pnpm test src/entities/community && npx tsc --noEmit`, Expected: PASS, 오류 없음
- [ ] **Step 5: Commit**

```bash
git add src/entities/community
git commit -m "feat: 게시글 조회 기록 API 호출과 조회수 캐시 동기화를 추가한다"
```

---

### Task 5: 상세 화면 연결

**Files:**
- Create: `src/widgets/community-detail/lib/shouldRefetchPersonalization.ts`
- Test: `src/widgets/community-detail/lib/shouldRefetchPersonalization.test.ts`
- Modify: `src/widgets/community-detail/ui/PostDetailPanel.tsx:50-57` (기존 재조회 effect 블록 교체)
- Modify: `src/entities/community/model/usePostDetail.ts:5-15, 19-30` (주석), `src/views/community/ui/CommunityDetailPageClient.tsx:33-35` (주석)

**Interfaces:**
- Consumes: `AuthStatus`·`useAuthStore((s) => s.status)` (Task 2), `useRecordPostView` (Task 4), `Post.viewerAuthenticated` (Task 4)
- Produces: `shouldRefetchPersonalization(status: AuthStatus, post: Pick<Post, "viewerAuthenticated"> | undefined): boolean` — `status === "authenticated"` 이고 `post` 가 있으며 `!post.viewerAuthenticated` 일 때만 `true`. 필드가 없는 응답(백엔드 배포 전)도 `true` 로 본다.

- [ ] **Step 1: 실패하는 테스트 작성**

```ts
it.each([
  ["authenticated", { viewerAuthenticated: false }, true],
  ["authenticated", { viewerAuthenticated: true }, false],
  ["authenticated", {} as Pick<Post, "viewerAuthenticated">, true],
  ["authenticated", undefined, false],
  ["anonymous", { viewerAuthenticated: false }, false],
  ["unknown", { viewerAuthenticated: false }, false],
  ["unverified", { viewerAuthenticated: false }, false],
] as const)("%s / %o → %s", (status, post, expected) => {
  expect(shouldRefetchPersonalization(status, post)).toBe(expected);
});
```

- [ ] **Step 2: 실패 확인** — Run: `pnpm test src/widgets/community-detail`, Expected: FAIL — 모듈 없음
- [ ] **Step 3: 구현과 `PostDetailPanel` 연결** — 기존 블록(서버 조회 무인증 주석 + `hasInitialPost`·`userId` + `useEffect`)을 다음으로 바꾼다. 판단은 `initialPost` 가 아니라 **현재 `post`** 로 한다(SSR 실패로 클라이언트가 직접 받은 경우까지 덮는다). 재조회 뒤에도 값이 그대로면 의존성이 바뀌지 않아 반복되지 않는다.

```tsx
const authStatus = useAuthStore((s) => s.status);
// 세션이 확정된 뒤에 기록해야 토큰이 만료돼 있던 회원도 회원으로 한 번만 센다.
useRecordPostView(postId, authStatus !== "unknown");
const needsPersonalization = shouldRefetchPersonalization(authStatus, post);
useEffect(() => {
  if (needsPersonalization) refetch();
}, [needsPersonalization, refetch]);
```

- [ ] **Step 4: 주석 정리** — `usePostDetail.ts`: "GET 이 조회수를 올린다"는 근거를 지우고, 조회수는 `POST …/views` 가 세며 invalidate 금지는 불필요한 재요청을 막기 위해 유지한다고 고친다. `initialData` 설명의 "새로고침 한 번에 조회수가 2 씩 오른다"·"로그인 상태 보정은 refetch" 문구를 "SSR 이 로그인 토큰을 실어 개인화하고, 서버가 비로그인으로 응답했을 때만 재조회한다"로 고친다. `CommunityDetailPageClient.tsx` 의 "조회수 +1" 문구를 "같은 키를 공유해 요청이 늘지 않는다"로 고친다.
- [ ] **Step 5: 전체 검증** — Run: `pnpm test && pnpm lint && npx tsc --noEmit && pnpm build`, Expected: 모두 성공
- [ ] **Step 6: 수동 확인** (로컬 lol-server 에 백엔드 계획 적용 후)
  - 비로그인으로 상세 열기 → `POST /v1/community/posts/{id}/views` 1회, 응답에 `Set-Cookie: visitorId`. 새로고침 반복 → 조회수 그대로.
  - 로그인 상태로 상세 열기 → 상세 GET 재요청 없음, `POST …/views` 1회. 새로고침 → 그대로.
  - `accessToken` 쿠키 삭제 후 새로고침 → `/v1/members/me` 401 → refresh → `/me` 200 → `POST …/views` → 상세 GET 1회. 추천·북마크 상태가 맞게 보이고 조회수는 오르지 않음(같은 회원).
  - 글 수정 화면 진입 → `POST …/views` 없음.
- [ ] **Step 7: Commit**

```bash
git add src/widgets/community-detail src/entities/community/model/usePostDetail.ts src/views/community/ui/CommunityDetailPageClient.tsx
git commit -m "feat: 상세 화면에서 세션 확정 뒤 조회를 기록하고 필요할 때만 재조회한다"
```
