# infra:client:oauth

OAuth2 / RSO (Riot Sign-On) 어댑터 (driven adapter). Authorization URL 생성, code → token 교환, 외부 프로바이더에서 사용자 정보 조회를 담당. 도메인의 `OAuthAuthorizationPort`, `OAuthClientPort`, `OAuthProviderClient`, `RiotAccountPort` 를 구현.

## Boundaries

- 허용: `core:lol-server-domain`, `spring-boot-starter-web` (RestClient)
- 금지: 다른 인프라 모듈 직접 호출, JWT 발급 (그건 `infra:api:JwtTokenAdapter` 책임)
- 신규 OAuth 프로바이더는 `OAuthProviderClient` 인터페이스 구현 + `OAuthProvider` enum 추가로만 추가 — `OAuthClientAdapter` 는 자동 라우팅

## Layout

- `adapter/oauth/OAuthClientAdapter.java` — `OAuthClientPort` 의 facade, `OAuthProviderClient` 빈들을 `OAuthProvider` enum 으로 라우팅
- `adapter/oauth/OAuthAuthorizationAdapter.java` — Authorization URL builder (`OAuthAuthorizationPort` 구현)
- `adapter/oauth/RiotRsoClient.java` — Riot RSO 전용 `OAuthProviderClient` + `RiotAccountPort` 동시 구현체
- `adapter/oauth/OAuthTokenExchanger.java` — code → access_token 교환 공통 로직 (모든 프로바이더 공유)
- `adapter/oauth/dto/OAuthTokenResponse.java` — 토큰 응답 DTO
- `adapter/oauth/config/OAuthProperties.java`, `OAuthClientConfig.java` — `oauth.providers.<name>.*` 프로퍼티 + `oauthRestClient` 빈

## Key Files

- `adapter/oauth/OAuthClientAdapter.java` — Strategy 패턴 reference (`Map<OAuthProvider, OAuthProviderClient>` 자동 조립)
- `adapter/oauth/RiotRsoClient.java` — 두 개의 out port 동시 구현 (`OAuthProviderClient`, `RiotAccountPort`) — 같은 RSO 토큰을 두 용도로 재활용
- `adapter/oauth/OAuthTokenExchanger.java` — POST `application/x-www-form-urlencoded` 토큰 교환의 공통 코드
- `adapter/oauth/OAuthAuthorizationAdapter.java` — Authorization URL 구성 패턴
- `adapter/oauth/config/OAuthProperties.java` — 프로바이더 설정 매핑 (`oauth.providers.<name>.client-id/secret/authorization-uri/...`)

## Common Modifications

- **새 프로바이더 추가** (예: GitHub):
  1. 도메인 `OAuthProvider` enum 에 `GITHUB` 추가
  2. `adapter/oauth/<NewProvider>OAuthClient.java implements OAuthProviderClient` 작성
  3. `getProvider()` 가 새 enum 반환, `getUserInfo(code, redirectUri)` 에서 `OAuthTokenExchanger.exchange(...)` 재사용 후 사용자 정보 호출
  4. 환경 yaml 에 `oauth.providers.github.*` 추가
- **신규 사용자 정보 필드**: `OAuthUserInfo` 도메인 모델 + 각 ProviderClient 의 `getUserInfo` 구현 동시 수정
- **토큰 교환 로직 변경**: `OAuthTokenExchanger` 만 수정 — 모든 프로바이더에 일괄 적용

## Failure Patterns / Gotchas

- ❌ `OAuthClientAdapter` 의 라우팅 맵에서 신규 프로바이더 빠짐 — 런타임에 `OAUTH_LOGIN_FAILED`
  ✅ `OAuthProviderClient` 빈을 새로 추가하면 생성자에서 자동 등록됨 (`@Component` 만 붙이면 됨)
- ❌ `OAuthProvider` 매직 스트링 (`"RIOT"`, `"GOOGLE"`)
  ✅ `OAuthProvider.RIOT.name()`, `OAuthProvider.GOOGLE.name()` (도메인 VO enum 사용)
- ❌ 토큰 교환 응답 (`OAuthTokenResponse`) null 체크 없이 `.getAccessToken()` 호출 — NPE
  ✅ `OAuthTokenExchanger` / `RiotRsoClient.fetchPuuid` 처럼 null/필드 누락 시 `CoreException(OAUTH_LOGIN_FAILED, "메시지")` 명시적 변환
- ❌ Authorization URL 에 `state` 파라미터 누락 — CSRF 취약
  ✅ `OAuthAuthorizationAdapter` 가 항상 `state` 를 쿼리에 포함, `state` 자체는 `infra:persistence:redis` 의 `OAuthStateRedisAdapter` 가 발급/검증
- ❌ `RestClient` 자동주입 사용 — `infra:api` 가 정의한 일반 `RestClient` 와 혼동
  ✅ `OAuthClientConfig` 가 `oauthRestClient` 라는 별도 빈으로 등록, 어댑터는 그걸 주입

## Cross-Module Dependencies

- depends on: `core:lol-server-domain` (OAuth 관련 out port + `OAuthProvider` VO)
- consumed by: `app:application` (런타임 빈 주입)
- 협력: `infra:persistence:redis` 의 `OAuthStateRedisAdapter` 가 state 토큰 저장/검증, `infra:api` 의 `OAuth2AuthenticationSuccessHandler` 가 이 어댑터들의 결과로 JWT 발급

## See Also

- [core:lol-server-domain](../../../core/lol-server-domain/CLAUDE.md) — `domain/member/application/port/out/` 의 OAuth 관련 port
- [infra/api](../../api/CLAUDE.md) — 실제 OAuth 진입점 (`controller/security/`)
- [persistence/redis](../../persistence/redis/CLAUDE.md) — `OAuthStateRedisAdapter` (state 발급/검증)
- [client/lol-repository](../lol-repository/CLAUDE.md) — Riot API 일반 호출 (인증 무관)
