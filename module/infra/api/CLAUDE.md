# infra:api

REST 컨트롤러 (driving adapter). HTTP 요청을 받아 도메인 UseCase를 호출하고, ReadModel을 컨트롤러 응답 record로 변환해 `ApiResponse<T>` 로 감싸 반환한다. Spring Security + OAuth2 + JWT 진입점도 여기 있다.

## Boundaries

- 허용: `core:lol-server-domain`, `core:enum`, Spring Web/Security/OAuth2-client/Validation, `springdoc-openapi`, `jjwt`
- 금지: 직접 `JpaRepository`/`RedisTemplate` 호출, 도메인 객체(`DuoPost`, `Member` 등) 를 응답으로 노출 — 항상 `*ReadModel → *Response` 변환을 거친다
- 컨트롤러는 UseCase 인터페이스(in port) 만 의존. PersistencePort 같은 out port 직접 의존 금지.

## Layout

- `controller/<domain>/` — 도메인별 `*Controller.java`, `request/`, `response/`
- `controller/security/` — Spring Security 설정, JWT 필터, OAuth2 핸들러, `AuthenticatedMember` (principal)
- `controller/support/response/` — `ApiResponse`, `SliceResponse`, `PageResponse`, `ResultType`
- `controller/config/` — `ObjectMapperConfig`, `WebConfig` (CORS, MVC 설정)
- `controller/CoreExceptionAdvice.java` — 전역 예외 핸들러 (`@RestControllerAdvice`)
- `src/docs/asciidoc/` — RestDocs 결과물을 묶는 AsciiDoc index/include 파일

## Key Files

- `controller/duo/DuoPostController.java` — 표준 컨트롤러 reference (UseCase 호출 → ReadModel → Response, RESTful 상태 코드)
- `controller/CoreExceptionAdvice.java` — `CoreException`/Validation/Generic 3단계 핸들링, ErrorType → HTTP status 매핑
- `controller/support/response/ApiResponse.java` — 모든 응답 래퍼 (`success(data)`, `error(errorType)`)
- `controller/security/SecurityConfig.java` — 인증/인가 체인, OAuth2 endpoint 등록
- `controller/security/JwtAuthenticationFilter.java`, `JwtTokenAdapter.java` — Access/Refresh 토큰 검증과 `OAuthAuthorizationPort` 구현
- `src/test/java/.../docs/RestDocsSupport.java` — Standalone MockMvc + RestDocs 베이스 (모든 `*ControllerTest` 가 상속)

## Common Modifications

- **새 엔드포인트 추가**:
  1. `domain` 모듈의 in port (UseCase) 에 메서드 추가 → 서비스에 구현
  2. `controller/<domain>/` 에 `Request` record (`@Valid`, `toCommand()` 메서드) 와 `Response` record (`from(readModel)`) 작성
  3. 컨트롤러에서 UseCase 호출 → `ResponseEntity.<status>().body(ApiResponse.success(...))`
  4. RestDocs 테스트 작성 후 `./gradlew :module:infra:api:asciidoctor` 로 문서 재생성, `src/docs/asciidoc/` 에 `.adoc` include 추가
- **새 에러 응답**: `support/error/ErrorType` 에 enum 추가 → 도메인에서 `throw new CoreException(...)`. 컨트롤러/Advice 코드는 안 건드린다.
- **인증 필요 엔드포인트**: `@AuthenticationPrincipal AuthenticatedMember member` 파라미터 추가, `member.memberId()` 사용. SecurityConfig 의 permitAll 화이트리스트와 충돌 없는지 확인.

## Failure Patterns / Gotchas

- ❌ `@RestController` 에서 도메인 객체 직접 반환 (`return ResponseEntity.ok(duoPost)`)
  ✅ `*ReadModel → *Response.from(readModel)` 로 변환 후 반환
- ❌ `return ResponseEntity.ok(...)` 로 생성도 200 반환
  ✅ 생성 `201 CREATED`, 조회/수정 `200 OK`, 삭제 `204 NO_CONTENT` (`ResponseEntity<Void>` + `noContent().build()`)
- ❌ 컨트롤러에서 boolean 체크 후 `throw new CoreException(...)`
  ✅ 도메인 객체 guard 메서드 (`validate*`) 가 던지게 둔다 — Advice 가 받아서 변환
- ❌ RestDocs 테스트만 추가하고 `asciidoctor` 미실행 — `bootJar` 시 docs 누락
  ✅ 테스트 후 `./gradlew :module:infra:api:asciidoctor` 실행, `src/docs/asciidoc/` 에 신규 snippet include
- ❌ Response 를 클래스로 (`class XxxResponse { ... }`) — 모든 컨트롤러 응답은 Java `record`
- ❌ `toCommand()` 없이 `Request` 를 그대로 Service 에 전달 — 도메인은 컨트롤러 DTO 모름

## Cross-Module Dependencies

- depends on: `core:lol-server-domain` (UseCase, ReadModel, Command), `core:enum`
- consumed by: `app:application` (bootJar 만 의존, 그 외 직접 사용처 없음)
- 런타임에 인프라 어댑터 모듈 (persistence/postgresql, redis, client/oauth 등) 이 도메인 out port 를 채워주지 않으면 빈 주입이 실패한다 — 컨트롤러 자체는 어댑터 모듈을 직접 참조하지 않는다

## Quick Commands

```bash
./gradlew :module:infra:api:test                # 컨트롤러/Security/RestDocs 테스트
./gradlew :module:infra:api:asciidoctor         # RestDocs HTML 재생성 (테스트 변경 후 필수)
./gradlew :module:infra:api:checkstyleMain      # checkstyle 단독 실행
```

## See Also

- [core:lol-server-domain](../../core/lol-server-domain/CLAUDE.md) — 호출하는 UseCase 의 출처
- [client/oauth](../client/oauth/CLAUDE.md) — `OAuth2AuthenticationSuccessHandler` 와 토큰 교환 어댑터의 협력 관계
- `src/docs/asciidoc/index.adoc` — 생성된 API 문서의 진입점
- 빌드 명령: `./gradlew :module:infra:api:asciidoctor` (RestDocs HTML 생성)
