# OAuth2 소셜 로그인

- **날짜**: 2026-04-02
- **관련 모듈**: `module/infra/api`, `module/core/lol-server-domain`

---

## 개요

OAuth2 소셜 로그인은 Spring Security OAuth2 Client가 처리합니다.
브라우저 리다이렉트 기반으로 동작하며, 별도의 API 호출 없이 URL 접근만으로 인증이 시작됩니다.

### 지원 프로바이더

| 프로바이더 | 로그인 URL | 프로토콜 | 인증 방식 | 비고 |
|-----------|-----------|---------|----------|------|
| Google | `GET /oauth2/authorize/google` | OIDC | `client_secret_post` | Spring Security 빌트인 지원 |
| Riot (RSO) | `GET /oauth2/authorize/riot` | OIDC | `client_secret_basic` | 커스텀 프로바이더 등록 |

---

## 공통 로그인 흐름

모든 프로바이더는 동일한 **Authorization Code Grant** 흐름을 따릅니다.

```
┌──────────┐          ┌──────────┐          ┌──────────────┐
│ 프론트엔드 │          │   서버    │          │ OAuth 프로바이더│
└────┬─────┘          └────┬─────┘          └──────┬───────┘
     │                     │                       │
     │ 1. GET /oauth2/     │                       │
     │    authorize/{id}   │                       │
     ├────────────────────►│                       │
     │                     │                       │
     │                     │ 쿠키에 state 저장      │
     │                     │ (CSRF 방지)            │
     │                     │                       │
     │ 2. 302 Redirect     │                       │
     │◄────────────────────┤                       │
     │                     │                       │
     │ 3. 프로바이더        │                       │
     │    로그인 페이지     │                       │
     ├─────────────────────────────────────────────►│
     │                     │                       │
     │    (사용자 인증)     │                       │
     │                     │                       │
     │ 4. 콜백 리다이렉트   │                       │
     │    ?code=...&state=..                       │
     │◄─────────────────────────────────────────────┤
     │                     │                       │
     │ 5. GET /login/oauth2│                       │
     │    /code/{id}       │                       │
     ├────────────────────►│                       │
     │                     │                       │
     │                     │ 6. code → token 교환   │
     │                     ├──────────────────────►│
     │                     │◄──────────────────────┤
     │                     │   access_token,       │
     │                     │   id_token            │
     │                     │                       │
     │                     │ 7. userinfo 조회       │
     │                     ├──────────────────────►│
     │                     │◄──────────────────────┤
     │                     │   사용자 정보          │
     │                     │                       │
     │                     │ 8. Member 조회/생성    │
     │                     │    + JWT 토큰 발급     │
     │                     │                       │
     │ 9. 302 Redirect     │                       │
     │    → 프론트엔드      │                       │
     │    #accessToken=... │                       │
     │◄────────────────────┤                       │
     │                     │                       │
```

### 성공 응답

인증 성공 시 프론트엔드 콜백 URL의 **fragment(#)** 에 토큰 정보가 포함됩니다.

리다이렉트 URL 예시: `http://localhost:3000/auth/callback#accessToken=eyJ...&refreshToken=eyJ...&expiresIn=1800`

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| accessToken | String | JWT 액세스 토큰 |
| refreshToken | String | JWT 리프레시 토큰 |
| expiresIn | Number | 액세스 토큰 만료 시간 (초) |

### 실패 응답

인증 실패 시 프론트엔드 콜백 URL의 **fragment(#)** 에 에러 정보가 포함됩니다.

리다이렉트 URL 예시: `http://localhost:3000/auth/callback#error=OAUTH_LOGIN_FAILED`

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| error | String | 에러 코드 (예: `OAUTH_LOGIN_FAILED`) |

---

## Google OAuth2 로그인

### 흐름 상세

```
1. 프론트엔드 → GET /oauth2/authorize/google
   └─ Spring Security가 Google 로그인 페이지로 리다이렉트
   └─ scope: openid, email, profile

2. 사용자가 Google 계정으로 로그인

3. Google → GET /login/oauth2/code/google?code={code}&state={state}
   └─ Spring Security가 자동으로 콜백 처리
   └─ code를 access_token + id_token으로 교환
   └─ Google Userinfo API 호출 → 사용자 정보 획득

4. GoogleOAuth2UserInfoExtractor가 OAuthUserInfo 생성
   └─ provider: "GOOGLE"
   └─ providerId: sub (Google 고유 ID)
   └─ email, nickname, profileImageUrl

5. MemberAuthService.loginWithOAuthUserInfo() 처리
   └─ 기존 회원 조회 또는 신규 회원 생성
   └─ JWT 액세스/리프레시 토큰 발급

6. 프론트엔드 콜백 URL로 토큰과 함께 리다이렉트
```

### Google 설정

| 항목 | 값 |
|------|-----|
| Authorization URI | `https://accounts.google.com/o/oauth2/v2/auth` |
| Token URI | `https://oauth2.googleapis.com/token` |
| UserInfo URI | `https://www.googleapis.com/oauth2/v3/userinfo` |
| Scope | `openid, email, profile` |
| User Name Attribute | `sub` |
| Client Authentication | `client_secret_post` (body에 credentials 포함) |

### Google UserInfo 응답 예시

```json
{
  "sub": "1234567890",
  "email": "user@gmail.com",
  "email_verified": true,
  "name": "홍길동",
  "picture": "https://lh3.googleusercontent.com/...",
  "given_name": "길동",
  "family_name": "홍",
  "locale": "ko"
}
```

---

## Riot RSO (Riot Sign On) 로그인

Riot RSO는 Spring Security OAuth2 Client에 **커스텀 프로바이더**로 등록되어 있습니다.
Google과 동일한 리다이렉트 방식으로 동작하지만, 추가적으로 Riot Account API를 호출하여 게임 계정 정보(PUUID, 게임명, 태그)를 획득합니다.

### 흐름 상세

```
1. 프론트엔드 → GET /oauth2/authorize/riot
   └─ Spring Security가 Riot 로그인 페이지로 리다이렉트
   └─ scope: openid, cpid

2. 사용자가 Riot 계정으로 로그인

3. Riot → GET /login/oauth2/code/riot?code={code}&state={state}
   └─ Spring Security가 자동으로 콜백 처리
   └─ code를 access_token + id_token으로 교환 (Basic Auth 사용)
   └─ Riot Userinfo API 호출 → sub, cpid 획득

4. CustomOidcUserService가 Riot Account API 추가 호출
   └─ GET /riot/account/v1/accounts/me (access_token 사용)
   └─ puuid, gameName, tagLine 획득
   └─ OAuth2User attributes에 병합

5. RiotOAuth2UserInfoExtractor가 OAuthUserInfo 생성
   └─ provider: "RIOT"
   └─ providerId: sub (Riot 고유 ID)
   └─ puuid, gameName, tagLine
   └─ nickname: "{gameName}#{tagLine}" 형식

6. MemberAuthService.loginWithOAuthUserInfo() 처리
   └─ 기존 회원 조회 또는 신규 회원 생성
   └─ JWT 액세스/리프레시 토큰 발급

7. 프론트엔드 콜백 URL로 토큰과 함께 리다이렉트
```

### Riot RSO 설정

| 항목 | 값 |
|------|-----|
| Issuer URI (OIDC Discovery) | `https://auth.riotgames.com` |
| Authorization URI | `https://auth.riotgames.com/authorize` (자동 설정) |
| Token URI | `https://auth.riotgames.com/token` (자동 설정) |
| UserInfo URI | `https://auth.riotgames.com/userinfo` (자동 설정) |
| Account API URI | `https://americas.api.riotgames.com/riot/account/v1/accounts/me` |
| Scope | `openid, cpid` |
| User Name Attribute | `sub` |
| Client Authentication | `client_secret_basic` (Authorization 헤더에 Base64 인코딩) |

### Google과의 차이점

| 항목 | Google | Riot RSO |
|------|--------|----------|
| 프로바이더 등록 | Spring Security 빌트인 | `issuer-uri` 기반 OIDC Discovery |
| Token 교환 인증 | Body에 client_id/secret 포함 | Basic Auth 헤더 (Base64) |
| 추가 API 호출 | 불필요 (userinfo로 충분) | Account API 호출 필요 (puuid, gameName, tagLine) |
| Scope | `openid, email, profile` | `openid, cpid` |
| 사용자 식별자 | `sub` (Google ID) | `sub` (Riot RSO ID) |
| nickname 생성 | Google 이름 (`name`) | `gameName#tagLine` 형식 |

---

## 아키텍처 및 클래스 구조

### 클래스 다이어그램

```
┌─────────────────────────────────────────────────────────────────┐
│                    Spring Security Filter Chain                  │
│                                                                  │
│  ┌──────────────────────┐    ┌──────────────────────────────┐   │
│  │JwtAuthenticationFilter│    │OAuth2LoginAuthenticationFilter│   │
│  │                      │    │ (Spring Security 내장)         │   │
│  │ /login/oauth2/,      │    │                              │   │
│  │ /oauth2/ 경로 제외    │    │ /login/oauth2/code/* 처리    │   │
│  └──────────────────────┘    └──────────┬───────────────────┘   │
│                                         │                        │
└─────────────────────────────────────────┼────────────────────────┘
                                          │
                    ┌─────────────────────┼─────────────────────┐
                    │                     ▼                     │
                    │        ┌────────────────────────┐         │
                    │        │   CustomOidcUserService  │         │
                    │        │   (OidcUserService 확장) │         │
                    │        └──────────┬─────────────┘         │
                    │                   │                        │
                    │         ┌─────────┴──────────┐            │
                    │         │                    │            │
                    │    Google 요청           Riot 요청        │
                    │    (super 호출)       (super + Account    │
                    │         │              API 추가 호출)     │
                    │         ▼                    ▼            │
                    │  ┌─────────────┐   ┌──────────────────┐  │
                    │  │  OidcUser   │   │  OidcUser        │  │
                    │  │  (sub,email,│   │  (sub,puuid,     │  │
                    │  │   name,...) │   │   gameName,      │  │
                    │  └──────┬──────┘   │   tagLine,...)   │  │
                    │         │          └────────┬─────────┘  │
                    │         │                   │             │
                    │  ┌──────▼───────────────────▼───────────┐ │
                    │  │  OAuth2AuthenticationSuccessHandler   │ │
                    │  │                                       │ │
                    │  │  extractors (Map<registrationId,      │ │
                    │  │              OAuth2UserInfoExtractor>) │ │
                    │  │   ├─ "google" → GoogleOAuth2User...   │ │
                    │  │   └─ "riot"   → RiotOAuth2User...     │ │
                    │  └──────────────────┬────────────────────┘ │
                    │                     │                      │
                    │                     ▼                      │
                    │           ┌──────────────────┐             │
                    │           │   OAuthUserInfo    │             │
                    │           │ (provider,         │             │
                    │           │  providerId,       │             │
                    │           │  email, nickname,  │             │
                    │           │  puuid, gameName,  │             │
                    │           │  tagLine)          │             │
                    │           └────────┬───────────┘             │
                    │                    │                         │
                    │                    ▼                         │
                    │           ┌──────────────────┐              │
                    │           │ MemberAuthService  │              │
                    │           │                    │              │
                    │           │ → Member 조회/생성  │              │
                    │           │ → JWT 토큰 발급     │              │
                    │           └────────┬───────────┘              │
                    │                    │                          │
                    │                    ▼                          │
                    │         프론트엔드로 토큰 리다이렉트            │
                    │                                               │
                    └───────────────────────────────────────────────┘
```

### 핵심 클래스 설명

#### SecurityConfig

- **위치**: `controller.security.SecurityConfig`
- **역할**: Spring Security 필터 체인 구성

OAuth2 로그인 관련 설정:
- `authorizationEndpoint`: 로그인 시작 URL (`/oauth2/authorize`)
- `authorizationRequestRepository`: 쿠키 기반 인증 요청 저장소
- `oidcUserService`: OIDC 사용자 정보 로드 서비스 (커스텀)
- `successHandler` / `failureHandler`: 인증 성공/실패 후처리

#### CookieOAuth2AuthorizationRequestRepository

- **위치**: `controller.security.CookieOAuth2AuthorizationRequestRepository`
- **역할**: OAuth2 인증 요청(state)을 쿠키에 저장/복원

Stateless 세션 환경에서 HTTP 세션 대신 **쿠키**를 사용하여 OAuth2 인증 요청을 관리합니다.
`state` 파라미터를 쿠키에 저장하여 CSRF 공격을 방지합니다.

| 동작 | 설명 |
|------|------|
| 저장 | 인증 요청 시작 시 `OAuth2AuthorizationRequest`를 JSON → Base64 인코딩하여 쿠키에 저장 |
| 복원 | 콜백 시 쿠키에서 인증 요청을 읽어 `state` 검증 |
| 삭제 | 인증 완료(성공/실패) 후 쿠키 삭제 |
| 쿠키 설정 | `HttpOnly`, `Secure`(HTTPS일 때만), `SameSite=Lax`, TTL 300초 |

#### CustomOidcUserService

- **위치**: `controller.security.oauth2.CustomOidcUserService`
- **역할**: OIDC 사용자 정보 로드 (프로바이더별 분기 처리)

Spring Security의 `OidcUserService`를 확장하여 프로바이더별 커스텀 로직을 추가합니다.

- **Google**: `super.loadUser()` 호출 후 그대로 반환 (기본 OIDC 흐름)
- **Riot**: `super.loadUser()` 호출 후 **Riot Account API를 추가 호출**하여 `puuid`, `gameName`, `tagLine`을 `OidcUser` attributes에 병합

```
super.loadUser()             → 기본 OIDC userinfo 처리 (sub, cpid 등)
  │
  ├─ registrationId="google" → 그대로 반환
  │
  └─ registrationId="riot"   → Account API 추가 호출
                                 GET /riot/account/v1/accounts/me
                                 → puuid, gameName, tagLine 병합
                                 → 확장된 OidcUser 반환
```

#### OAuth2UserInfoExtractor (인터페이스)

- **위치**: `controller.security.oauth2.OAuth2UserInfoExtractor`
- **역할**: Spring Security `OAuth2User` → 도메인 `OAuthUserInfo` 변환

| 메서드 | 반환 | 설명 |
|--------|------|------|
| `getRegistrationId()` | `String` | 이 추출기가 처리하는 프로바이더 ID (예: `"google"`, `"riot"`) |
| `extract(OAuth2User)` | `OAuthUserInfo` | OAuth2User의 attributes에서 도메인 객체로 변환 |

#### GoogleOAuth2UserInfoExtractor

- **위치**: `controller.security.oauth2.GoogleOAuth2UserInfoExtractor`
- **역할**: Google OAuth2User → OAuthUserInfo 변환

| OAuth2User attribute | OAuthUserInfo 필드 | 설명 |
|---------------------|-------------------|------|
| `sub` | `providerId` | Google 고유 사용자 ID |
| `email` | `email` | 이메일 주소 |
| `name` | `nickname` | 표시 이름 |
| `picture` | `profileImageUrl` | 프로필 이미지 URL |

#### RiotOAuth2UserInfoExtractor

- **위치**: `controller.security.oauth2.RiotOAuth2UserInfoExtractor`
- **역할**: Riot OAuth2User → OAuthUserInfo 변환

| OAuth2User attribute | OAuthUserInfo 필드 | 설명 |
|---------------------|-------------------|------|
| `sub` | `providerId` | Riot RSO 고유 사용자 ID |
| `puuid` | `puuid` | Riot 게임 PUUID (Account API에서 획득) |
| `gameName` | `gameName` | Riot 게임 닉네임 (Account API에서 획득) |
| `tagLine` | `tagLine` | Riot 게임 태그 (Account API에서 획득) |
| (생성) | `nickname` | `{gameName}#{tagLine}` 형식으로 조합 |

#### OAuth2AuthenticationSuccessHandler

- **위치**: `controller.security.OAuth2AuthenticationSuccessHandler`
- **역할**: OAuth2 인증 성공 후 JWT 토큰 발급 및 프론트엔드 리다이렉트

처리 흐름:
1. `OAuth2AuthenticationToken`에서 `registrationId`와 `OAuth2User` 추출
2. `registrationId`에 매핑된 `OAuth2UserInfoExtractor`를 찾아 `OAuthUserInfo` 생성
3. `MemberAuthUseCase.loginWithOAuthUserInfo()`로 회원 조회/생성 및 토큰 발급
4. 프론트엔드 콜백 URL로 토큰과 함께 리다이렉트

#### OAuth2AuthenticationFailureHandler

- **위치**: `controller.security.OAuth2AuthenticationFailureHandler`
- **역할**: OAuth2 인증 실패 시 에러 리다이렉트

인증 실패 시 `{frontendCallbackUrl}#error=OAUTH_LOGIN_FAILED`로 리다이렉트합니다.
쿠키에 저장된 인증 요청도 함께 삭제합니다.

#### MemberAuthService

- **위치**: `domain.member.application.MemberAuthService`
- **역할**: OAuth 로그인 비즈니스 로직

`loginWithOAuthUserInfo()` 메서드에서:
1. `(provider, providerId)` 조합으로 기존 회원 조회
2. 회원이 없으면 신규 생성 (`Member.createFromOAuth()`)
3. 회원이 있으면 마지막 로그인 시간 업데이트
4. JWT 액세스/리프레시 토큰 발급 및 반환

---

## 커스텀 OAuth 프로바이더 추가 가이드

새로운 OAuth 프로바이더를 추가하려면 아래 단계를 따릅니다.

### 1단계: YAML 설정 추가 (`api-local.yml`)

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          {provider-id}:                                    # 프로바이더 식별자
            client-id: ${CLIENT_ID:}
            client-secret: ${CLIENT_SECRET:}
            scope: openid, ...                              # 필요한 scope
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
            client-authentication-method: client_secret_basic  # 또는 client_secret_post
            authorization-grant-type: authorization_code
        provider:
          {provider-id}:
            issuer-uri: https://auth.example.com            # OIDC Discovery 지원 시
            # 또는 수동 설정:
            # authorization-uri: https://auth.example.com/authorize
            # token-uri: https://auth.example.com/token
            # user-info-uri: https://auth.example.com/userinfo
            user-name-attribute: sub
```

### 2단계: OAuth2UserInfoExtractor 구현

`OAuth2UserInfoExtractor` 인터페이스를 구현하여 프로바이더의 사용자 정보를 `OAuthUserInfo`로 변환합니다.

```java
@Component
public class NewProviderExtractor implements OAuth2UserInfoExtractor {

    @Override
    public String getRegistrationId() {
        return "{provider-id}";  // YAML의 registration ID와 일치
    }

    @Override
    public OAuthUserInfo extract(OAuth2User oauth2User) {
        Map<String, Object> attributes = oauth2User.getAttributes();
        return OAuthUserInfo.builder()
                .provider("NEW_PROVIDER")
                .providerId((String) attributes.get("sub"))
                .email((String) attributes.get("email"))
                .nickname((String) attributes.get("name"))
                .build();
    }
}
```

### 3단계: CustomOidcUserService 확장 (필요 시)

프로바이더의 userinfo 엔드포인트 외에 추가 API 호출이 필요한 경우, `CustomOidcUserService`에 분기 로직을 추가합니다.

```java
// CustomOidcUserService.loadUser() 내부
if ("{provider-id}".equals(registrationId)) {
    return enrichWithAdditionalInfo(userRequest, oidcUser);
}
```

### 4단계: OAuthProvider enum 추가

`domain.member.domain.vo.OAuthProvider`에 새 프로바이더를 추가합니다.

```java
public enum OAuthProvider {
    GOOGLE,
    RIOT,
    NEW_PROVIDER  // 추가
}
```

### 변경 파일 요약

| 파일 | 변경 내용 |
|------|----------|
| `api-local.yml` | OAuth2 client registration/provider 설정 |
| `{Provider}OAuth2UserInfoExtractor.java` | 사용자 정보 추출기 (신규) |
| `CustomOidcUserService.java` | 추가 API 호출이 필요한 경우만 수정 |
| `OAuthProvider.java` | enum 값 추가 |
| `SecurityConfig.java` | 변경 불필요 (자동 감지) |

> **참고**: `SecurityConfig`, `OAuth2AuthenticationSuccessHandler`는 수정할 필요가 없습니다.
> `OAuth2UserInfoExtractor`의 `@Component` 등록과 `getRegistrationId()` 매핑으로 자동 연결됩니다.
