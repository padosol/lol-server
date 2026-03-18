# Code Review: OAuth 인증 및 회원 관리 기능

- **날짜**: 2026-03-17
- **브랜치**: `main` (미커밋 변경사항)
- **변경 파일**: 51개 신규 + 7개 수정 (약 1,929 additions)

---

## 변경 개요

이 변경은 크게 **4가지**로 구성됩니다:

1. **OAuth 로그인** — Google OAuth2 + Riot RSO를 통한 소셜 로그인 (`OAuthClientAdapter` → `GoogleOAuthClient` / `RiotRsoClient`)
2. **JWT 기반 인증** — Access/Refresh Token 발급, 검증, 갱신 (`JwtTokenAdapter`, `JwtAuthenticationFilter`, `SecurityConfig`)
3. **회원 관리** — 회원 생성, 프로필 조회, Riot 계정 연동/해제 (`MemberService`, `MemberController`)
4. **인프라 구성** — Redis Refresh Token 저장, PostgreSQL 회원/연동 영속성, OAuth 클라이언트 모듈 분리

---

## 긍정적인 부분

- **헥사고날 아키텍처가 일관되게 적용됨.** `TokenPort`, `RefreshTokenPort`, `OAuthClientPort`, `MemberPersistencePort` 등 포트 인터페이스를 통해 도메인이 인프라로부터 완전히 분리됨
- **Read Model 패턴 준수.** `AuthTokenReadModel`, `MemberReadModel`, `RiotAccountLinkReadModel`이 도메인 계층에, `AuthTokenResponse`, `MemberResponse`가 API 계층에 올바르게 배치됨
- **OAuth 클라이언트 별도 모듈 분리 (`module/infra/client/oauth`).** Provider별 구현체를 독립 모듈로 분리하여 확장성 확보
- **`MemberServiceTest`의 테스트 커버리지가 양호.** 로그인, 토큰 갱신, 로그아웃, Riot 계정 연동/해제/조회, 프로필 조회 등 핵심 시나리오를 BDDMockito 패턴으로 검증

---

## 이슈 및 제안

### [Critical-1] SecurityConfig — `anyRequest().permitAll()`은 보안 취약점

`SecurityConfig.java:41`:

```java
.authorizeHttpRequests(auth -> auth
        .requestMatchers("/api/v1/**").permitAll()
        .requestMatchers("/api/auth/**").permitAll()
        .requestMatchers("/docs/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
        .requestMatchers("/api/members/**").authenticated()
        .anyRequest().permitAll()    // ← 문제
)
```

마지막 `.anyRequest().permitAll()`로 인해 명시적으로 정의되지 않은 **모든 엔드포인트가 인증 없이 접근 가능**합니다. 새 엔드포인트를 추가할 때 `requestMatchers`를 빠뜨리면 자동으로 공개됩니다. `.anyRequest().authenticated()`로 변경하여 화이트리스트 방식(허용할 것만 명시)으로 전환해야 합니다.

### [Critical-2] Refresh Token Redis TTL 계산 버그

`MemberService.java:160`:

```java
private AuthTokenReadModel generateTokens(Member member) {
    // ...
    refreshTokenPort.save(member.getId(), refreshToken, tokenPort.getAccessTokenExpiry() * 28);
    // ...
}
```

`tokenPort.getAccessTokenExpiry()`는 Access Token의 만료 시간(초)을 반환합니다. 이를 `* 28`로 곱해 Refresh Token TTL로 사용하는데, **의도와 실제 값이 일치하는지 검증이 필요**합니다. 예를 들어 Access Token expiry가 `1800`(30분)이면 `1800 * 28 = 50,400`초 = **14시간**이 됩니다. Refresh Token이 보통 14일인 점을 감안하면 TTL이 크게 부족합니다. `JwtProperties.refreshTokenExpiry` 값을 직접 사용하거나, `TokenPort`에 `getRefreshTokenExpiry()` 메서드를 추가하여 명시적으로 전달하는 것이 안전합니다.

### [Critical-3] `JwtTokenAdapter`의 `@Getter`로 SecretKey 외부 노출

`JwtTokenAdapter.java:24-25`:

```java
@lombok.Getter
private SecretKey secretKey;
```

`@Getter`로 인해 `getSecretKey()` 메서드가 public으로 노출됩니다. `SecretKey`는 토큰 서명/검증에 사용되는 핵심 보안 자산으로, 외부에서 접근 가능하면 **토큰 위조가 가능**합니다. 현재 `JwtAuthenticationFilter`가 `JwtTokenAdapter`를 직접 참조하므로 `getSecretKey()`에 접근 가능한 상태입니다. `@Getter`를 제거하고, 필요한 검증 기능은 메서드(`validateToken`, `getMemberIdFromToken`)를 통해서만 제공해야 합니다.

### [Critical-4] `/api/auth/logout`이 permitAll인데 `@AuthenticationPrincipal` 사용

`SecurityConfig.java:37`에서 `/api/auth/**`는 `permitAll()`이므로 인증 없이 접근 가능합니다.
그러나 `AuthController.java:49`에서:

```java
@PostMapping("/logout")
public ApiResponse<?> logout(@AuthenticationPrincipal AuthenticatedMember member) {
    memberAuthUseCase.logout(member.memberId());  // member가 null이면 NPE
    return ApiResponse.success();
}
```

인증되지 않은 사용자가 `/api/auth/logout`을 호출하면 `member`가 `null`이 되어 **`NullPointerException`이 발생**합니다. 해결 방안:
- `/api/auth/logout`을 `authenticated()`로 변경하거나
- `null` 체크를 추가 (`if (member == null) return ApiResponse.error(...)`)

---

### [High-1] `refreshToken()`이 `@Transactional(readOnly = true)`인데 Redis 쓰기 수행

`MemberService.java:58`:

```java
@Transactional(readOnly = true)
public AuthTokenReadModel refreshToken(TokenRefreshCommand command) {
    // ... 검증 로직 ...
    return generateTokens(member);  // ← 내부에서 refreshTokenPort.save() 호출
}
```

`generateTokens()` 내부에서 `refreshTokenPort.save()`를 호출하여 Redis에 새 Refresh Token을 저장합니다. `@Transactional(readOnly = true)`는 JPA flush를 방지하므로 Redis 쓰기 자체는 동작하지만, **트랜잭션 의미론과 실제 동작이 불일치**합니다. `@Transactional`로 변경하는 것이 올바릅니다.

### [High-2] `JwtAuthenticationFilter`가 구체 클래스 `JwtTokenAdapter`에 직접 의존 (DIP 위반)

`JwtAuthenticationFilter.java:27`:

```java
private final JwtTokenAdapter jwtTokenAdapter;
```

헥사고날 아키텍처에서 어댑터(Filter)는 포트 인터페이스에 의존해야 합니다. 현재 `JwtAuthenticationFilter`가 `JwtTokenAdapter` 구체 클래스에 직접 의존하여, **테스트 시 Mock 교체가 어렵고** 아키텍처 규칙을 위반합니다. `TokenPort`에 `getRoleFromToken()` 메서드를 추가하고, 필터에서 `TokenPort`를 주입받도록 변경하는 것을 권장합니다.

### [High-3] Request DTO에 입력 유효성 검증 없음

`OAuthLoginRequest.java`, `TokenRefreshRequest.java`, `RiotLinkRequest.java` 모두 `@NotBlank`, `@NotNull` 등의 Bean Validation 어노테이션이 없습니다:

```java
public record OAuthLoginRequest(
        String code,        // null/빈 문자열 허용
        String redirectUri  // null/빈 문자열 허용
) {}
```

Controller에서도 `@Valid`를 사용하지 않아 **null이나 빈 값이 서비스 계층까지 전파**됩니다. `code`가 `null`이면 OAuth 토큰 교환 시 외부 API에 의미 없는 요청을 보내게 됩니다. 최소한 필수 필드에 `@NotBlank`를 추가하고 Controller에 `@Valid`를 적용해야 합니다.

### [High-4] `OAuthProvider`를 String으로 전달하여 enum 활용 미흡

`AuthController.java:29`에서 provider를 하드코딩된 문자열로 전달합니다:

```java
OAuthLoginCommand command = OAuthLoginCommand.builder()
        .provider("GOOGLE")  // ← 문자열 하드코딩
        .code(request.code())
        .redirectUri(request.redirectUri())
        .build();
```

`OAuthProvider` enum이 존재하지만 `OAuthLoginCommand.provider`가 `String` 타입이어서 활용되지 않습니다. `OAuthClientAdapter.getUserInfo()`에서도 `String provider`로 비교합니다. **`OAuthLoginCommand.provider`를 `OAuthProvider` enum으로 변경**하면 타입 안전성이 확보되고 컴파일 타임에 오류를 잡을 수 있습니다.

---

### [Medium-1] OAuth 응답 파싱 시 필수 필드 null 체크 없음

`GoogleOAuthClient.java:81-87`:

```java
return OAuthUserInfo.builder()
        .provider(OAuthProvider.GOOGLE.name())
        .providerId((String) response.get("id"))       // null 가능
        .email((String) response.get("email"))          // null 가능
        .nickname((String) response.get("name"))        // null 가능
        .profileImageUrl((String) response.get("picture"))
        .build();
```

Google API가 `"id"` 필드를 누락하면 `providerId`가 `null`이 되어, `MemberService`에서 `findByOAuthProviderAndProviderId(provider, null)`로 조회됩니다. `RiotRsoClient`도 동일하게 `puuid`, `gameName`, `tagLine`이 null일 수 있습니다. **`providerId`/`puuid` 등 필수 필드에 대해 null 체크 후 `CoreException`을 발생**시켜 fail-fast 해야 합니다.

### [Medium-2] CORS 허용 오리진 하드코딩

`SecurityConfig.java:52-56`:

```java
configuration.setAllowedOrigins(List.of(
        "http://localhost:3000",
        "http://localhost:8080",
        "http://lol-ui:3000",
        "https://metapick.me"
));
```

오리진 목록이 소스코드에 하드코딩되어 있어 환경별 변경 시 재배포가 필요합니다. **application.yml의 프로파일별 설정**으로 외부화하면 환경(local/dev/prod)에 따라 유연하게 관리할 수 있습니다.

### [Medium-3] `RiotAccountLinkUseCase`에 `getMyProfile()` 메서드가 포함됨

`MemberController.java:34`:

```java
MemberReadModel readModel = riotAccountLinkUseCase.getMyProfile(member.memberId());
```

`getMyProfile()`은 회원 프로필 조회 기능으로, `RiotAccountLinkUseCase`의 책임 범위(Riot 계정 연동)와 맞지 않습니다. `MemberAuthUseCase`로 이동하거나 별도의 `MemberQueryUseCase` 포트를 만드는 것이 **인터페이스 분리 원칙(ISP)**에 부합합니다.

---

## 누락 항목

- **`/api/auth/refresh` 엔드포인트의 Rate Limiting 미적용**: 프로젝트에 Bucket4j가 설정되어 있으나, 토큰 갱신 엔드포인트에 적용되지 않았습니다. 무제한 호출 시 토큰 남용 가능성이 있습니다.
- **Access Token 로그아웃 시 무효화 미처리**: `logout()` 메서드가 Redis의 Refresh Token만 삭제하고, 이미 발급된 Access Token은 만료까지 유효합니다. Token Blacklist 또는 짧은 만료 시간(현재 설정 확인 필요)으로 보완이 필요합니다.
- **OAuth 클라이언트 모듈의 테스트 부재**: `GoogleOAuthClient`와 `RiotRsoClient`에 대한 단위/통합 테스트가 없습니다. `MockRestServiceServer`를 활용한 토큰 교환, 사용자 정보 조회, 에러 시나리오 테스트를 권장합니다.
- **`SecurityConfigTest` 범위 부족**: 현재 SecurityConfig 테스트가 있으나, logout NPE 시나리오나 미정의 엔드포인트 접근 제어 검증이 포함되었는지 확인이 필요합니다.

---

## 테스트 관련

- `MemberServiceTest`에서 `loginWithOAuth`, `refreshToken`, `logout`, `linkRiotAccount`, `unlinkRiotAccount`, `getLinkedAccounts`, `getMyProfile`의 정상/예외 케이스를 커버하고 있어 기본적인 품질이 확보됨
- `JwtTokenAdapterTest`에서 토큰 생성, 검증, 만료 토큰 처리를 검증함
- 다만 **`refreshToken()` 메서드에서 저장된 토큰과 요청 토큰 불일치 시 예외 발생 테스트**가 있는지 확인 필요 (토큰 탈취 시나리오 방어)

---

## 전체 평가

아키텍처적으로 잘 설계된 변경입니다. 헥사고날 아키텍처의 포트/어댑터 분리, Read Model 패턴, 모듈 분리가 프로젝트 컨벤션에 맞게 적용되었습니다. 그러나 **보안 관련 Critical 이슈 4건**(`anyRequest().permitAll()`, TTL 계산 버그, SecretKey 노출, logout NPE)은 프로덕션 배포 전 반드시 수정해야 합니다. High 이슈 중 입력 유효성 검증과 `@Transactional` 설정도 함께 해결하는 것을 권장합니다.
