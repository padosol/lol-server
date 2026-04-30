# OAuth 소셜 계정 연동 시 새 회원 생성 버그 수정

## Context

로그인된 사용자가 소셜 계정을 연동하면, 기존 계정에 연결되지 않고 **새로운 Member가 생성**되는 버그.
원인: Spring Security의 `OAuth2LoginAuthenticationFilter`가 콜백 처리 시 `removeAuthorizationRequest()`를 먼저 호출하여 `ConcurrentHashMap`에서 `OAuth2AuthorizationRequest`를 삭제. 이후 SuccessHandler에서 `loadAuthorizationRequest()`를 호출하면 이미 삭제된 상태라 `null` 반환 → `link_member_id` 유실 → LOGIN 모드로 폴백 → 새 Member 생성.

## 수정 대상 파일

### 1. `CookieOAuth2AuthorizationRequestRepository.java` (핵심 수정)
**경로:** `module/infra/api/src/main/java/com/example/lolserver/controller/security/CookieOAuth2AuthorizationRequestRepository.java`

**변경 내용:**
- `removeAuthorizationRequest()` 호출 시 삭제된 `OAuth2AuthorizationRequest`를 `HttpServletRequest.setAttribute()`로 저장
- `loadAuthorizationRequest()`에서 store miss 시 request attribute에서 폴백 조회

```java
// 상수 추가
private static final String REMOVED_AUTH_REQUEST_ATTR =
        CookieOAuth2AuthorizationRequestRepository.class.getName() + ".REMOVED_REQUEST";

// removeAuthorizationRequest() - 삭제 후 request attribute에 보존
OAuth2AuthorizationRequest authRequest = entry.request();
request.setAttribute(REMOVED_AUTH_REQUEST_ATTR, authRequest);  // 추가
return authRequest;

// loadAuthorizationRequest() - store miss 시 폴백
Object removed = request.getAttribute(REMOVED_AUTH_REQUEST_ATTR);
if (removed instanceof OAuth2AuthorizationRequest removedRequest) {
    return removedRequest;
}
```

### 2. `OAuth2AuthenticationSuccessHandler.java` (정리)
**경로:** `module/infra/api/src/main/java/com/example/lolserver/controller/security/OAuth2AuthenticationSuccessHandler.java`

**변경 내용:**
- `finally` 블록의 불필요한 `removeAuthorizationRequest()` 호출 제거 (Spring Security가 이미 처리)

## 수정하지 않는 파일

- `LinkOAuth2AuthorizationRequestResolver.java` - 정상 작동
- `SocialAccountLinkTokenStore.java` - 정상 작동
- `MemberAuthService.java` - 도메인 로직 정상, 버그는 인프라 계층

## 테스트

### 단위 테스트: `CookieOAuth2AuthorizationRequestRepositoryTest.java` (신규)
1. `loadAfterRemove_returnsCachedRequest` - remove 후 같은 request에서 load 시 정상 반환 확인 (핵심 회귀 테스트)
2. `loadAfterRemove_differentRequest_returnsNull` - 다른 request 객체에서는 null 반환 확인
3. `load_beforeRemove_returnsFromStore` - 정상 흐름 확인
4. `load_noState_returnsNull` - state 없을 때 null 반환

### 빌드 검증
```bash
./gradlew clean build
```
