# RSO(Riot Sign On) OAuth2 연동 트러블슈팅 기록

## 1. 개요

### 1.1 목적
Spring Security OAuth2 Client를 통한 Riot Sign On(RSO) 소셜 로그인 통합 과정에서 발생한 문제들의 원인 분석, 디버깅 과정, 해결 방법을 상세히 기록한다.

### 1.2 기술 스택
- Spring Boot 3.3.6
- Spring Security 6.3.5
- Spring Security OAuth2 Client 6.3.5
- Nimbus JOSE + JWT (spring-security-oauth2-jose 포함)

### 1.3 인프라 환경 제약사항
RSO 클라이언트 등록 시 다음 제약이 있다:
- **redirect URI에 `localhost` 사용 불가**
- **redirect URI에 포트 번호 지정 불가**
- 따라서 `http://local.metapick.me/login/oauth2/code/riot`을 redirect URI로 등록
- `/etc/hosts`에 `127.0.0.1 local.metapick.me` 추가
- nginx로 `local.metapick.me:80` → `localhost:8100` upstream 구성

### 1.4 RSO OAuth2 플로우 (Spring Security 기반)
```
[사용자 브라우저]
    │
    ▼ (1) GET /oauth2/authorize/riot
[Spring Security - OAuth2AuthorizationRequestRedirectFilter]
    │  Authorization Request를 저장소에 저장
    │  RSO authorize URL 구성 (client_id, scope, state, redirect_uri, nonce)
    ▼ (2) 302 Redirect → https://auth.riotgames.com/authorize?...
[RSO 로그인 페이지]
    │  사용자가 Riot 계정으로 로그인
    ▼ (3) 302 Redirect → http://local.metapick.me/login/oauth2/code/riot?code=...&state=...
[nginx] → [Spring Security - OAuth2LoginAuthenticationFilter]
    │  (4) 저장소에서 Authorization Request 조회 (state로 매칭)
    │  (5) POST https://auth.riotgames.com/token (code → access_token 교환)
    │  (6) ID Token (JWT) 디코딩 및 검증
    │  (7) CustomOidcUserService.loadUser() → Riot Account API 호출
    │  (8) OAuth2AuthenticationSuccessHandler → JWT 토큰 발급 → 프론트엔드 리다이렉트
    ▼
[프론트엔드 콜백]
    accessToken, refreshToken 수신
```

---

## 2. 문제 1: `authorization_request_not_found`

### 2.1 증상

RSO 로그인 페이지에서 정상적으로 로그인한 후, 콜백 처리 단계에서 다음 에러 발생:

```
org.springframework.security.oauth2.core.OAuth2AuthenticationException: [authorization_request_not_found]
    at o.s.s.oauth2.client.web.OAuth2LoginAuthenticationFilter.attemptAuthentication(...)
```

### 2.2 디버깅 과정

#### Step 1: 디버그 로깅 활성화

`api-local.yml`에 Spring Security OAuth2 TRACE 레벨 로깅 추가:

```yaml
logging:
  level:
    org.springframework.security: DEBUG
    org.springframework.security.oauth2: TRACE
    org.springframework.security.oauth2.client: TRACE
    org.springframework.web.client.RestTemplate: DEBUG
```

#### Step 2: CookieOAuth2AuthorizationRequestRepository에 디버그 로그 추가

쿠키 저장/로드 시점에 로그를 추가하여 쿠키가 정상적으로 왕복하는지 확인:

```java
// loadAuthorizationRequest()에 추가
log.debug("[OAuth2 Cookie] loadAuthorizationRequest - URL: {}, 쿠키 존재: {}, authRequest: {}",
        request.getRequestURL(), request.getCookies() != null, authRequest != null);
```

#### Step 3: 로그 분석

```
# (1) 인증 요청 시 - 쿠키 저장 성공
[nio-8100-exec-3] [OAuth2 Cookie] saveAuthorizationRequest
    - redirectUri: http://local.metapick.me/login/oauth2/code/riot
    - state: qb3uhsm9vM7DwC6e9yXNvypXp0vGjtdGlXX9Sozkc4I=
    - isSecure: false

# (2) RSO 로그인 후 콜백 - 쿠키 조회 실패
[nio-8100-exec-5] OAuth2LoginAuthenticationFilter: Failed to process authentication request
    → authorization_request_not_found
```

콜백 시점에 `loadAuthorizationRequest` 디버그 로그가 출력되지 않음 → 쿠키 자체가 전달되지 않았음을 의미.

### 2.3 근본 원인

**쿠키 기반 저장소의 도메인 불일치 문제.**

기존 `CookieOAuth2AuthorizationRequestRepository`는 OAuth2 Authorization Request를 브라우저 쿠키(`oauth2_auth_request`)에 저장했다. 그런데:

1. 사용자가 `localhost:8100/oauth2/authorize/riot`으로 접근
2. Spring Security가 `oauth2_auth_request` 쿠키를 **`localhost` 도메인**에 설정
3. RSO 로그인 성공 후 `http://local.metapick.me/login/oauth2/code/riot`으로 리다이렉트
4. 브라우저가 `local.metapick.me`에 요청 → **`localhost`에 설정된 쿠키는 전달되지 않음**
5. `OAuth2LoginAuthenticationFilter`가 `removeAuthorizationRequest()` 호출 → `null` 반환
6. `authorization_request_not_found` 에러 throw

```
localhost:8100                      local.metapick.me
    │                                       │
    ├── Cookie 설정: oauth2_auth_request    │
    │   Domain: localhost                   │
    │                                       │
    │   ........RSO 로그인........          │
    │                                       │
    │                     콜백 요청 ────────►│
    │                     Cookie: (없음!)    │
    │                     → authorization_request_not_found
```

쿠키는 **도메인에 바인딩**되므로, `localhost`에서 설정한 쿠키는 `local.metapick.me` 요청에 포함되지 않는다. `local.metapick.me`를 통해 접근하더라도, nginx 프록시 뒤에서 쿠키 도메인 처리가 일관되지 않을 수 있다.

### 2.4 해결 방법

**쿠키 기반 저장소를 `state` 파라미터 기반 인메모리 저장소로 교체.**

OAuth2 `state` 파라미터는 Authorization Request → RSO → Callback URL 전체 과정에서 쿼리스트링으로 보존된다. 이를 키로 사용하면 쿠키/도메인과 완전히 독립적으로 동작한다.

```
localhost:8100                      local.metapick.me
    │                                       │
    ├── InMemory 저장: state=abc123         │
    │   (서버 메모리, 도메인 무관)            │
    │                                       │
    │   ........RSO 로그인........          │
    │                                       │
    │                     콜백: ?state=abc123 ──►│
    │                     InMemory 조회: state=abc123 → 성공!
```

#### 변경 전 (쿠키 기반)
```java
// CookieOAuth2AuthorizationRequestRepository.java (Before)
public class CookieOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    private static final String COOKIE_NAME = "oauth2_auth_request";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void saveAuthorizationRequest(
            OAuth2AuthorizationRequest authorizationRequest, ...) {
        String serialized = serialize(authorizationRequest); // JSON → Base64
        Cookie cookie = new Cookie(COOKIE_NAME, serialized);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(request.isSecure());
        cookie.setAttribute("SameSite", "Lax");
        cookie.setMaxAge(300);
        response.addCookie(cookie);
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(...) {
        OAuth2AuthorizationRequest req = getCookie(request); // 쿠키에서 역직렬화
        deleteCookie(response);
        return req;
    }

    // + serialize(), deserialize(), getCookie(), deleteCookie() 메서드들
}
```

#### 변경 후 (state 기반 인메모리)
```java
// CookieOAuth2AuthorizationRequestRepository.java (After)
public class CookieOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    private static final int EXPIRE_SECONDS = 300;
    private final ConcurrentHashMap<String, AuthorizationRequestEntry> store =
            new ConcurrentHashMap<>();

    @Override
    public void saveAuthorizationRequest(
            OAuth2AuthorizationRequest authorizationRequest, ...) {
        evictExpired();
        String state = authorizationRequest.getState();
        store.put(state, new AuthorizationRequestEntry(
                authorizationRequest,
                Instant.now().plusSeconds(EXPIRE_SECONDS)));
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(...) {
        String state = request.getParameter("state"); // 콜백 URL의 state 파라미터
        AuthorizationRequestEntry entry = store.remove(state);
        if (entry == null || entry.isExpired()) return null;
        return entry.request();
    }

    private record AuthorizationRequestEntry(
            OAuth2AuthorizationRequest request, Instant expiresAt) {
        boolean isExpired() { return Instant.now().isAfter(expiresAt); }
    }
}
```

**핵심 변경점:**
- 쿠키 직렬화/역직렬화(JSON → Base64) 코드 전체 제거
- `ConcurrentHashMap`으로 thread-safe 인메모리 저장
- `state` 파라미터를 키로 사용하여 도메인 독립적 동작
- 300초 TTL + `evictExpired()`로 메모리 누수 방지

### 2.5 적용 결과

```
[OAuth2 State] 저장 - state: P99G9fEJYsKl55PDBWt4_LspVF__0rNCnSl2SOaoWBk=,
    redirectUri: http://local.metapick.me/login/oauth2/code/riot

[OAuth2 State] 조회 성공 - state: P99G9fEJYsKl55PDBWt4_LspVF__0rNCnSl2SOaoWBk=
```

`authorization_request_not_found` 에러 해결 확인.

---

## 3. 문제 2: OIDC Discovery 의존성

### 3.1 기존 설정

```yaml
# api-local.yml (Before)
provider:
  riot:
    issuer-uri: https://auth.riotgames.com
    user-name-attribute: sub
```

`issuer-uri`만 설정하면 Spring Security가 `https://auth.riotgames.com/.well-known/openid-configuration`에서 다음 endpoint들을 자동 discovery한다:
- `authorization_endpoint`
- `token_endpoint`
- `jwks_uri`
- `userinfo_endpoint`

### 3.2 잠재적 문제

RSO의 OIDC Discovery 응답이 Spring Security의 기대와 다를 경우:
- 잘못된 endpoint URL이 사용될 수 있음
- Discovery 응답의 `token_endpoint_auth_methods_supported` 등이 클라이언트 설정을 override할 수 있음
- Discovery 서버 장애 시 전체 OAuth2 플로우가 실패

반면 Google provider는 모든 endpoint를 명시적으로 설정하여 안정적으로 동작하고 있었다:
```yaml
provider:
  google:
    authorization-uri: https://accounts.google.com/o/oauth2/v2/auth
    token-uri: https://oauth2.googleapis.com/token
    user-info-uri: https://www.googleapis.com/oauth2/v3/userinfo
```

### 3.3 해결 방법

RSO 공식 튜토리얼에 명시된 endpoint를 직접 설정:

```yaml
# api-local.yml (After)
provider:
  riot:
    authorization-uri: https://auth.riotgames.com/authorize
    token-uri: https://auth.riotgames.com/token
    jwk-set-uri: https://auth.riotgames.com/jwks.json
    user-info-uri: https://auth.riotgames.com/userinfo
    user-name-attribute: sub
```

각 endpoint는 RSO 튜토리얼의 다음 항목에 대응:
| Spring Security 설정 | RSO Endpoint | 용도 |
|---------------------|-------------|------|
| `authorization-uri` | `/authorize` | 인증 코드 획득 |
| `token-uri` | `/token` | 코드 → 토큰 교환 |
| `jwk-set-uri` | `/jwks.json` | ID Token JWT 서명 검증 키 |
| `user-info-uri` | `/userinfo` | 사용자 정보 조회 |

---

## 4. 문제 3: Token Endpoint 401 Unauthorized

### 4.1 증상

문제 1, 2 해결 후, 토큰 교환 단계에서 새로운 에러 발생:

```
org.springframework.security.oauth2.core.OAuth2AuthenticationException:
  [invalid_token_response] An error occurred while attempting to retrieve
  the OAuth 2.0 Access Token Response: 401 Unauthorized: [no body]
```

### 4.2 디버깅 과정

#### Step 1: `client_secret_basic`으로 요청 확인

```
HTTP POST https://auth.riotgames.com/token
Accept=[application/json, application/*+json]
Writing [{grant_type=[authorization_code],
          code=[YXAxOm...],
          redirect_uri=[http://local.metapick.me/login/oauth2/code/riot]}]
  as "application/x-www-form-urlencoded;charset=UTF-8"
Response 401 UNAUTHORIZED
```

`client_secret_basic` 방식에서는 credentials가 `Authorization: Basic` 헤더로 전달되므로 form body에는 표시되지 않는다. 헤더가 정상 전송되는지, credentials 값이 올바른지 확인이 필요했다.

#### Step 2: `client_secret_post`로 변경하여 credentials 노출 확인

진단을 위해 인증 방식을 `client_secret_post`로 임시 변경:

```yaml
client-authentication-method: client_secret_post  # 진단용 임시 변경
```

이렇게 하면 credentials가 form body에 포함되어 로그에서 확인 가능:

```
Writing [{grant_type=[authorization_code],
          code=[YXAxOm...],
          redirect_uri=[http://local.metapick.me/login/oauth2/code/riot],
          client_id=[14d91ae2-02d8-44d5-9575-9aa4092bd15b],
          client_secret=[sha256:TpLNKfzL6TRa3lLu+7LWfpK3ze5lBWlfEJ3trp61nyM=]}]
Response 401 UNAUTHORIZED
```

`client_id`는 정상 출력되었고, `client_secret`은 Spring 로깅에 의해 마스킹된 형태(`sha256:...`)로 표시되었다.

### 4.3 근본 원인

`RIOT_RSO_CLIENT_SECRET` 환경변수 값 문제. 사용자가 환경변수를 확인하여 올바른 값으로 설정한 후 해결되었다.

YAML 설정에서 환경변수 기본값이 빈 문자열로 되어 있어, 환경변수 미설정 시 빈 secret으로 인증을 시도하게 된다:
```yaml
client-secret: ${RIOT_RSO_CLIENT_SECRET:}  # 기본값: 빈 문자열
```

### 4.4 해결 후

`client-authentication-method`를 `client_secret_basic`으로 복원:

```yaml
client-authentication-method: client_secret_basic
```

RSO 튜토리얼에서 `client_secret_basic` 방식을 명시하고 있으며, 사용자도 RSO 클라이언트를 `client_secret_basic`으로 등록했음을 확인했다.

---

## 5. 문제 4: `JOSE header typ (type) id_token+jwt not allowed`

### 5.1 증상

토큰 교환 성공 후, ID Token 검증 단계에서 새로운 에러 발생:

```
[invalid_id_token] An error occurred while attempting to decode the Jwt:
  JOSE header typ (type) id_token+jwt not allowed
```

### 5.2 원인 분석

RSO가 발급하는 ID Token의 JWT JOSE 헤더:
```json
{
  "alg": "RS256",
  "typ": "id_token+jwt",   // ← RSO 고유 타입
  "kid": "..."
}
```

Spring Security의 `NimbusJwtDecoder`는 내부적으로 Nimbus JOSE 라이브러리의 `DefaultJOSEObjectTypeVerifier`를 사용하여 JWT 헤더의 `typ` 필드를 검증한다. 기본 설정은 다음 타입만 허용:
- `JWT` (표준)
- `null` (typ 필드 미지정)

RSO의 `id_token+jwt`는 OIDC 관련 명세에서 사용되는 유효한 타입이지만, Spring Security 기본 설정에는 포함되어 있지 않다.

### 5.3 해결 방법

`SecurityConfig`에 커스텀 `JwtDecoderFactory<ClientRegistration>` 빈을 등록하여 `id_token+jwt` 타입을 허용:

```java
// SecurityConfig.java
@Bean
public JwtDecoderFactory<ClientRegistration> idTokenDecoderFactory() {
    return clientRegistration -> {
        String jwkSetUri = clientRegistration.getProviderDetails()
                .getJwkSetUri();

        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder
                .withJwkSetUri(jwkSetUri)
                .jwtProcessorCustomizer(processor ->
                        processor.setJWSTypeVerifier(
                                new DefaultJOSEObjectTypeVerifier<>(
                                        JOSEObjectType.JWT,        // 표준 JWT
                                        new JOSEObjectType("id_token+jwt"), // RSO ID Token
                                        null)))                    // typ 미지정
                .build();

        jwtDecoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(
                        new JwtTimestampValidator(),
                        new OidcIdTokenValidator(clientRegistration)));

        return jwtDecoder;
    };
}
```

**설계 결정:**
- `OidcIdTokenDecoderFactory.setJwtProcessorCustomizer()`는 Spring Security 6.4+에서만 사용 가능하므로, 6.3.5 환경에서는 `JwtDecoderFactory<ClientRegistration>`를 직접 구현
- `NimbusJwtDecoder.withJwkSetUri().jwtProcessorCustomizer()`는 Spring Security 5.7+에서 사용 가능하여 문제없음
- 기존 `JWT`와 `null` 타입도 함께 허용하여 Google 등 다른 provider에도 호환성 유지
- `OidcIdTokenValidator`와 `JwtTimestampValidator`를 설정하여 기본 ID Token 검증 로직 유지

---

## 6. 수정 파일 요약

### 6.1 변경된 파일 목록

| 파일 | 변경 유형 | 관련 문제 |
|------|----------|----------|
| `module/infra/api/src/main/java/.../CookieOAuth2AuthorizationRequestRepository.java` | 전면 재작성 | 문제 1 |
| `module/infra/api/src/main/java/.../SecurityConfig.java` | 빈 추가 | 문제 4 |
| `module/infra/api/src/main/resources/api-local.yml` | 설정 변경 | 문제 2, 3 |
| `module/infra/api/src/main/java/.../CustomOidcUserService.java` | 디버그 로그 추가 | 디버깅용 |

### 6.2 `api-local.yml` 주요 변경

```yaml
# === registration.riot ===

# redirect-uri: 동적 → 하드코딩 (RSO 등록 URI와 일치)
# Before
redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
# After
redirect-uri: "http://local.metapick.me/login/oauth2/code/riot"

# === provider.riot ===

# issuer-uri → 명시적 endpoint (OIDC Discovery 제거)
# Before
issuer-uri: https://auth.riotgames.com
user-name-attribute: sub
# After
authorization-uri: https://auth.riotgames.com/authorize
token-uri: https://auth.riotgames.com/token
jwk-set-uri: https://auth.riotgames.com/jwks.json
user-info-uri: https://auth.riotgames.com/userinfo
user-name-attribute: sub
```

---

## 7. 남은 정리 작업

디버깅 완료 후 제거해야 할 항목:

1. **`api-local.yml`의 `logging` 블록** — 디버그 로깅 설정 제거
2. **`CustomOidcUserService.java`의 디버그 로그** — `log.debug("[OIDC]...")` 호출들 제거
3. **`CookieOAuth2AuthorizationRequestRepository.java`의 디버그 로그** — `log.debug("[OAuth2 State]...")` 호출들 제거 (선택)

---

## 8. 교훈 및 참고사항

### 8.1 RSO 고유 특성 (Spring Security 기본 설정과의 차이)

| 항목 | Spring Security 기본 | RSO |
|------|---------------------|-----|
| ID Token `typ` 헤더 | `JWT` 또는 미지정 | `id_token+jwt` |
| OIDC Discovery | 표준 `.well-known/openid-configuration` | 지원하나 명시적 설정 권장 |
| redirect URI | localhost 허용 | localhost 불가, 포트 지정 불가 |
| Token endpoint 인증 | 다양한 방식 지원 | `client_secret_basic` 또는 `client_secret_jwt` |

### 8.2 nginx 프록시 환경에서의 OAuth2 쿠키 이슈

reverse proxy 환경에서는 쿠키 기반 상태 저장에 주의가 필요하다:
- 쿠키는 도메인에 바인딩되므로, 접속 도메인과 콜백 도메인이 다르면 쿠키가 전달되지 않음
- 해결책: state 파라미터 기반 서버 메모리 저장, 또는 Redis 등 외부 저장소 활용
- 프로덕션 환경에서 스케일아웃 시 인메모리 저장소는 한계가 있으므로 Redis 기반으로 전환 고려 필요
