# JWT 인증 실패 시 302 → 401 반환으로 수정

## Context

JWT가 만료/무효/누락된 상태에서 인증 필요 API 엔드포인트(`/api/members/**`, `/api/duo/**` POST 등)를 호출하면, 401 JSON 응답 대신 **302 리다이렉트**가 반환된다.

**원인**: `SecurityConfig`에 `exceptionHandling` 설정이 없다. `.oauth2Login()`이 활성화되어 있으므로 Spring Security가 기본 `LoginUrlAuthenticationEntryPoint`를 사용하여 OAuth2 로그인 페이지로 302 리다이렉트한다.

**기대 동작**: REST API 인증 실패 시 `401 Unauthorized` + `ApiResponse.error(ErrorType.UNAUTHORIZED)` JSON 응답 반환.

## 수정 사항

### 1. `RestAuthenticationEntryPoint` 생성

- **위치**: `module/infra/api/src/main/java/com/example/lolserver/controller/security/RestAuthenticationEntryPoint.java`
- `AuthenticationEntryPoint` 구현
- 인증되지 않은 요청에 대해 401 상태코드 + `ApiResponse.error(ErrorType.UNAUTHORIZED)` JSON 반환
- `ObjectMapper`로 JSON 직렬화 (Spring Security 필터 체인에서 동작하므로 `@RestControllerAdvice` 미적용)

```java
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(),
                ApiResponse.error(ErrorType.UNAUTHORIZED));
    }
}
```

### 2. `SecurityConfig`에 `exceptionHandling` 추가

- **파일**: `module/infra/api/src/main/java/com/example/lolserver/controller/security/SecurityConfig.java`
- `RestAuthenticationEntryPoint` 주입
- `.exceptionHandling()` 블록 추가하여 `authenticationEntryPoint` 설정

```java
.exceptionHandling(exception -> exception
        .authenticationEntryPoint(restAuthenticationEntryPoint)
)
```

## 수정 대상 파일

| 파일 | 변경 내용 |
|------|----------|
| `controller/security/RestAuthenticationEntryPoint.java` | **신규** - 401 JSON 응답 반환 EntryPoint |
| `controller/security/SecurityConfig.java` | `exceptionHandling` 설정 추가 |

## 검증

1. `./gradlew build` - 빌드 성공 확인
2. 인증 없이 `/api/members/**` 등 인증 필요 엔드포인트 호출 시 401 JSON 응답 반환 확인
3. 기존 OAuth2 로그인 플로우(성공/실패 리다이렉트) 정상 동작 확인
